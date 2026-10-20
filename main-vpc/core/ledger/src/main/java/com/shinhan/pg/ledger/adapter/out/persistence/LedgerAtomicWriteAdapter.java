package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.LedgerPostedEvent;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.ledger.adapter.out.persistence.outbox.LedgerOutboxStore;
import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.command.PostingInstruction;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.result.PostJournalResult;
import com.shinhan.pg.ledger.domain.model.Journal;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;
import com.shinhan.pg.ledger.domain.model.Posting;
import com.shinhan.pg.ledger.domain.policy.BalanceProtectionPolicy;
import com.shinhan.pg.ledger.domain.policy.JournalBalancePolicy;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * ADR 0001 6-A 실구현. 하나의 MariaDB 트랜잭션 안에서:
 *
 * <pre>
 * ① transactionId로 기존 journal 조회 — 있으면 기존 결과를 그대로 반환한다(멱등 200.
 *   TODO: 원장 동일 transactionId 결과 재조회와 클라이언트 중복 이체 409는 서로 다른 계약이다.
 *   409/기존 journal 응답의 최종 형식은 미정이다).
 * ② 대상 계좌를 external_account_id 정렬 순서대로 SELECT ... FOR UPDATE. 부재 계정은 그 자리에서
 *   0 잔액으로 lazy insert(지시서 §5-② 결정안: 계좌 개설 시 별도 provision 호출 없이 분개 시점 생성).
 *   모든 분개가 동일한 전역 정렬 순서로 잠그므로 분개 간 교착이 성립하지 않는다.
 * ③ JournalBalancePolicy로 통화별 합계 0 검증.
 * ④ BalanceProtectionPolicy로 출금 계좌 잔액 검사(부족 시 LedgerException → 422).
 * ⑤ journals + postings insert, 잔액 갱신.
 * ⑥ LedgerOutboxStore.append로 ledger.posted(WO-A3 payload)를 같은 트랜잭션에 기록.
 * 잠금 중 외부 호출은 없으며 외부 호출 자체가 없다(원장은 수신 전용).
 * </pre>
 *
 * <p>교착(1213)·lock wait timeout(1205)과 transaction_id unique 경합(DuplicateKeyException)만 최대 3회까지 소규모
 * backoff로 재시도한다(ADR 6-A "제한된 재시도 정책"). 업무 거절 (LedgerException·입력 오류)은 재시도하지 않는다.
 */
public final class LedgerAtomicWriteAdapter
        implements LedgerAtomicWritePort,
                com.shinhan.pg.ledger.application.port.out.OverseasLedgerPort {
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 50;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final LedgerOutboxStore outboxStore;
    private final JournalBalancePolicy journalBalancePolicy;
    private final BalanceProtectionPolicy balanceProtectionPolicy;

    public LedgerAtomicWriteAdapter(
            JdbcTemplate jdbc,
            TransactionTemplate transactionTemplate,
            LedgerOutboxStore outboxStore,
            JournalBalancePolicy journalBalancePolicy,
            BalanceProtectionPolicy balanceProtectionPolicy) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate);
        // READ COMMITTED: 이 트랜잭션의 읽기는 ① 멱등 조회(unique 제약이 경합을 보호)와
        // ② FOR UPDATE 잠금 읽기뿐이라 문장마다 최신 커밋 스냅샷을 써도 불변식이 유지된다.
        // MariaDB REPEATABLE READ는 잠금 대기 후 행이 갱신돼 있으면 1020(ER_CHECKREAD)으로
        // 실패하므로, 짧은 쓰기 트랜잭션의 격리 수준을 명시적으로 낮춘다(ADR 6-A 재시도와 병행).
        this.transactionTemplate.setIsolationLevel(
                org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.outboxStore = Objects.requireNonNull(outboxStore);
        this.journalBalancePolicy = Objects.requireNonNull(journalBalancePolicy);
        this.balanceProtectionPolicy = Objects.requireNonNull(balanceProtectionPolicy);
    }

    @Override
    public PostJournalResult postWithPessimisticLocks(PostJournalCommand command) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> doPost(command));
            } catch (RuntimeException error) {
                // unique(transaction_id) 경합은 재시도 시 ① 멱등 분기에서 기존 journal로 수렴한다.
                boolean retryable =
                        isLockContention(error) || error instanceof DuplicateKeyException;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw error;
                }
                backoff(attempt);
            }
        }
    }

    /**
     * MVP5-F06/F08: lock the stored reservation, apply one final financial effect, and atomically
     * publish convergence evidence.
     */
    @Override
    public void applyInstitutionResultWithPessimisticLocks(ApplyInstitutionResultCommand command) {
        retryOverseas(
                () -> {
                    var rows =
                            jdbc.queryForList(
                                    "select * from overseas_reservations where transfer_id = ? for"
                                            + " update",
                                    command.transactionId().toString());
                    if (rows.isEmpty())
                        throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
                    var row = rows.get(0);
                    if (!command.externalReference().equals(row.get("uetr")))
                        throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
                    String outcome = command.outcomeCode();
                    if (!java.util.Set.of("ACSC", "RJCT", "REVIEW").contains(outcome))
                        throw com.shinhan.pg.common.swiftmessages.SwiftProblem.invalid();
                    String previous = (String) row.get("outcome");
                    Object reviewValue = row.get("review_required");
                    boolean review =
                            Boolean.TRUE.equals(reviewValue)
                                    || (reviewValue instanceof Number number
                                            && number.intValue() != 0);
                    if (review || outcome.equals(previous)) return null;
                    if ("REVIEW".equals(outcome) || previous != null) {
                        jdbc.update(
                                "update overseas_reservations set review_required = true where"
                                        + " transfer_id = ?",
                                command.transactionId().toString());
                        overseasEvent(command, "REVIEW", null);
                        return null;
                    }
                    var transferId = command.transactionId();
                    UUID hold = UUID.fromString((String) row.get("hold_account_id"));
                    UUID target =
                            "RJCT".equals(outcome)
                                    ? UUID.fromString((String) row.get("source_account_id"))
                                    : UUID.fromString("00000000-0000-0000-0000-000000000505");
                    var amount = (BigDecimal) row.get("amount");
                    var posted =
                            doPost(
                                    new PostJournalCommand(
                                            UUID.randomUUID(),
                                            "swift",
                                            com.shinhan.pg.common.swiftmessages.SwiftIds.id(
                                                    transferId, "final"),
                                            List.of(
                                                    new PostingInstruction(
                                                            hold, amount.negate(), "USD"),
                                                    new PostingInstruction(
                                                            target, amount, "USD"))));
                    jdbc.update(
                            "update overseas_reservations set outcome = ?, final_journal_id = ?"
                                    + " where transfer_id = ?",
                            outcome,
                            posted.journalId().toString(),
                            transferId.toString());
                    overseasEvent(command, outcome, posted);
                    return null;
                });
    }

    /** The complete reservation/posting/outbox effect is retried as one local transaction. */
    @Override
    public PostJournalResult reserveOverseas(
            com.shinhan.pg.ledger.application.command.ReserveOverseasCommand c) {
        return retryOverseas(
                () -> {
                    var rows =
                            jdbc.queryForList(
                                    "select * from overseas_reservations where transfer_id = ? for"
                                            + " update",
                                    c.transferId().toString());
                    var txId =
                            com.shinhan.pg.common.swiftmessages.SwiftIds.id(c.transferId(), "hold");
                    if (!rows.isEmpty()) {
                        var row = rows.get(0);
                        if (!c.uetr().equals(row.get("uetr"))
                                || !c.actorUserId().toString().equals(row.get("actor_user_id"))
                                || !c.fromAccountId()
                                        .toString()
                                        .equals(row.get("source_account_id"))
                                || c.amount().compareTo((BigDecimal) row.get("amount")) != 0
                                || !c.currency().equals(row.get("currency")))
                            throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
                        return new PostJournalResult(
                                txId,
                                UUID.fromString((String) row.get("hold_journal_id")),
                                findPostedEventId(txId));
                    }
                    UUID hold =
                            com.shinhan.pg.common.swiftmessages.SwiftIds.id(
                                    c.transferId(), "holding-account");
                    var posted =
                            doPost(
                                    new PostJournalCommand(
                                            UUID.randomUUID(),
                                            "transfer",
                                            txId,
                                            List.of(
                                                    new PostingInstruction(
                                                            c.fromAccountId(),
                                                            c.amount().negate(),
                                                            "USD"),
                                                    new PostingInstruction(
                                                            hold, c.amount(), "USD"))));
                    jdbc.update(
                            "insert into overseas_reservations"
                                + " (transfer_id,uetr,actor_user_id,source_account_id,amount,currency,hold_account_id,hold_journal_id)"
                                + " values (?,?,?,?,?,?,?,?)",
                            c.transferId().toString(),
                            c.uetr(),
                            c.actorUserId().toString(),
                            c.fromAccountId().toString(),
                            c.amount(),
                            c.currency(),
                            hold.toString(),
                            posted.journalId().toString());
                    return posted;
                });
    }

    @Override
    public java.util.Optional<PostJournalResult> existingReservation(
            com.shinhan.pg.ledger.application.command.ReserveOverseasCommand c) {
        var rows =
                jdbc.queryForList(
                        "select * from overseas_reservations where transfer_id=?",
                        c.transferId().toString());
        if (rows.isEmpty()) return java.util.Optional.empty();
        var row = rows.get(0);
        if (!c.uetr().equals(row.get("uetr"))
                || !c.actorUserId().toString().equals(row.get("actor_user_id"))
                || !c.fromAccountId().toString().equals(row.get("source_account_id"))
                || c.amount().compareTo((BigDecimal) row.get("amount")) != 0
                || !c.currency().equals(row.get("currency")))
            throw com.shinhan.pg.common.swiftmessages.SwiftProblem.conflict();
        var txId = com.shinhan.pg.common.swiftmessages.SwiftIds.id(c.transferId(), "hold");
        return java.util.Optional.of(
                new PostJournalResult(
                        txId,
                        UUID.fromString((String) row.get("hold_journal_id")),
                        findPostedEventId(txId)));
    }

    private <T> T retryOverseas(java.util.function.Supplier<T> operation) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> operation.get());
            } catch (RuntimeException error) {
                if (attempt >= MAX_ATTEMPTS
                        || !(isLockContention(error) || error instanceof DuplicateKeyException))
                    throw error;
                backoff(attempt);
            }
        }
    }

    private void overseasEvent(
            ApplyInstitutionResultCommand c, String outcome, PostJournalResult posted) {
        var eventId = UUID.randomUUID();
        var event =
                new com.shinhan.pg.common.eventcontracts.OverseasLedgerAppliedEvent(
                        eventId,
                        c.transactionId(),
                        c.externalReference(),
                        outcome,
                        posted == null ? null : posted.transactionId(),
                        posted == null ? null : posted.journalId(),
                        Instant.now());
        outboxStore.append(
                new OutboxRecord(
                        UUID.randomUUID(),
                        eventId,
                        EventTopics.OVERSEAS_LEDGER_APPLIED,
                        c.transactionId().toString(),
                        EventCodec.toBytes(event)));
    }

    private PostJournalResult doPost(PostJournalCommand command) {
        // ① 멱등: 동일 transactionId의 기존 journal이 있으면 새로 반영하지 않고 그 결과를 반환한다.
        var existingJournalId = findJournalIdByTransactionId(command.transactionId());
        if (existingJournalId != null) {
            return new PostJournalResult(
                    command.transactionId(),
                    existingJournalId,
                    findPostedEventId(command.transactionId()));
        }

        // 입력 → 도메인 금액 변환. 통화 코드·스케일 오류는 업무 예외(400)로 변환한다.
        var amountsByAccount = toAmountsByAccount(command.postings());

        // ② 계좌 ID 정렬 순서로 SELECT ... FOR UPDATE. 없는 계정만 lazy insert
        //    (insert ignore를 먼저 쓰면 기존 행에 S 잠금이 걸려 FOR UPDATE와 교착이 생기므로
        //    조회→부재 시 생성 순서를 지킨다. 동시 최초 생성 경합은 unique 제약/교착 재시도로 수렴).
        var lockedAccounts = new LinkedHashMap<UUID, LedgerAccount>();
        for (var externalAccountId : amountsByAccount.keySet()) {
            var account =
                    lockOrProvision(
                            externalAccountId,
                            amountsByAccount.get(externalAccountId).get(0).currency());
            for (var money : amountsByAccount.get(externalAccountId)) {
                if (!account.balance().currency().equals(money.currency())) {
                    // 기존 원장 계정 통화와 분개 통화가 다르면 잘못된 요청이다(환산은 원장 책임이 아님).
                    throw invalid(
                            "currency mismatch for account "
                                    + externalAccountId
                                    + ": ledger="
                                    + account.balance().currency().getCurrencyCode()
                                    + " posting="
                                    + money.currency().getCurrencyCode());
                }
            }
            lockedAccounts.put(externalAccountId, account);
        }

        // ③ 통화별 합계 0 검증(도메인 정책).
        var journalId = UUID.randomUUID();
        var postedAt = Instant.now();
        var postings = new ArrayList<Posting>();
        for (var instruction : command.postings()) {
            postings.add(
                    new Posting(
                            UUID.randomUUID(),
                            journalId,
                            lockedAccounts.get(instruction.accountId()).id(),
                            toMoney(instruction)));
        }
        var journal = new Journal(journalId, command.transactionId(), postings, postedAt);
        journalBalancePolicy.validateBalanced(journal);

        // ④ 출금 계좌 잔액 검사(도메인 정책) — 계좌별 순적용액 기준.
        var netByAccount = new LinkedHashMap<UUID, Money>();
        for (var entry : amountsByAccount.entrySet()) {
            var net = entry.getValue().stream().reduce(Money::plus).orElseThrow();
            netByAccount.put(entry.getKey(), net);
            balanceProtectionPolicy.verifyAvailableBalance(lockedAccounts.get(entry.getKey()), net);
        }

        // ⑤ journal + postings insert, 잔액 갱신.
        jdbc.update(
                "insert into journals (id, transaction_id, posted_at) values (?, ?, ?)",
                journalId.toString(),
                command.transactionId().toString(),
                Timestamp.from(postedAt));
        for (var posting : postings) {
            jdbc.update(
                    "insert into postings (id, journal_id, ledger_account_id, signed_amount,"
                            + " currency) values (?, ?, ?, ?, ?)",
                    posting.id().toString(),
                    journalId.toString(),
                    posting.ledgerAccountId().toString(),
                    posting.signedAmount().amount(),
                    posting.signedAmount().currency().getCurrencyCode());
        }
        for (var entry : netByAccount.entrySet()) {
            var account = lockedAccounts.get(entry.getKey());
            var newBalance = account.balance().plus(entry.getValue());
            jdbc.update(
                    "update ledger_accounts set balance = ?, version = version + 1 where id = ?",
                    newBalance.amount(),
                    account.id().toString());
        }

        // ⑥ ledger.posted Outbox — 같은 로컬 트랜잭션. payload는 WO-A3 계약(식별자만).
        var eventId = UUID.randomUUID();
        var payload =
                EventCodec.toBytes(
                        new LedgerPostedEvent(
                                eventId, command.transactionId(), journalId, postedAt));
        outboxStore.append(
                new OutboxRecord(
                        UUID.randomUUID(),
                        eventId,
                        EventTopics.LEDGER_POSTED,
                        command.transactionId().toString(),
                        payload));
        return new PostJournalResult(command.transactionId(), journalId, eventId);
    }

    /** 계좌별 부호 있는 금액 목록. 키는 external_account_id 문자열 오름차순으로 정렬된다. */
    private Map<UUID, List<Money>> toAmountsByAccount(List<PostingInstruction> instructions) {
        var sorted = new TreeMap<String, UUID>();
        for (var instruction : instructions) {
            sorted.put(instruction.accountId().toString(), instruction.accountId());
        }
        var byAccount = new LinkedHashMap<UUID, List<Money>>();
        for (var externalAccountId : sorted.values()) {
            byAccount.put(externalAccountId, new ArrayList<>());
        }
        for (var instruction : instructions) {
            byAccount.get(instruction.accountId()).add(toMoney(instruction));
        }
        return byAccount;
    }

    private Money toMoney(PostingInstruction instruction) {
        try {
            return Money.of(
                    instruction.signedAmount(), Currency.getInstance(instruction.currency()));
        } catch (IllegalArgumentException | ArithmeticException error) {
            // 알 수 없는 통화 코드 또는 통화 scale 불일치(예: KRW 소수점)는 400이다.
            throw invalid("invalid amount/currency: " + error.getMessage());
        }
    }

    /**
     * 지시서 §5-② 결정안(lazy upsert). 잠금 조회를 먼저 하고 부재 시에만 0 잔액으로 생성한다. 생성한 행의 X 잠금은 이 트랜잭션이 커밋될 때까지
     * 유지되므로 재조회 없이 즉시 사용 가능하다. 동시에 같은 계정을 처음 생성하는 경합은 unique 제약(DuplicateKeyException) 또는 gap lock
     * 교착으로 표면화되고, 상위 재시도가 새 스냅샷에서 커밋된 행을 읽어 수렴한다.
     */
    private LedgerAccount lockOrProvision(UUID externalAccountId, Currency currency) {
        var existing = selectForUpdate(externalAccountId);
        if (existing != null) {
            return existing;
        }
        var id = UUID.randomUUID();
        jdbc.update(
                "insert into ledger_accounts"
                        + " (id, external_account_id, balance, currency, version)"
                        + " values (?, ?, ?, ?, 0)",
                id.toString(),
                externalAccountId.toString(),
                BigDecimal.ZERO,
                currency.getCurrencyCode());
        return new LedgerAccount(id, externalAccountId, Money.of(BigDecimal.ZERO, currency), 0L);
    }

    private LedgerAccount selectForUpdate(UUID externalAccountId) {
        var rows =
                jdbc.query(
                        "select id, external_account_id, balance, currency, version"
                                + " from ledger_accounts where external_account_id = ? for update",
                        (row, index) ->
                                new LedgerAccount(
                                        UUID.fromString(row.getString("id")),
                                        UUID.fromString(row.getString("external_account_id")),
                                        Money.of(
                                                row.getBigDecimal("balance"),
                                                Currency.getInstance(row.getString("currency"))),
                                        row.getLong("version")),
                        externalAccountId.toString());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private UUID findJournalIdByTransactionId(UUID transactionId) {
        var rows =
                jdbc.query(
                        "select id from journals where transaction_id = ?",
                        (row, index) -> UUID.fromString(row.getString("id")),
                        transactionId.toString());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private UUID findPostedEventId(UUID transactionId) {
        var rows =
                jdbc.query(
                        "select event_id from outbox_events"
                                + " where destination = ? and routing_key = ?"
                                + " order by created_at, id limit 1",
                        (row, index) -> UUID.fromString(row.getString("event_id")),
                        EventTopics.LEDGER_POSTED,
                        transactionId.toString());
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 재시도 가능한 잠금 경합 판별. MariaDB deadlock(1213)·lock wait timeout(1205)과 1020(ER_CHECKREAD —
     * REPEATABLE READ 스냅샷 이후 행 갱신; READ COMMITTED로 예방하지만 방어적으로 포함)을 커버한다. Spring 변환 예외와 cause
     * chain의 원시 SQLException을 모두 본다.
     */
    private static boolean isLockContention(Throwable error) {
        if (error instanceof PessimisticLockingFailureException) {
            return true;
        }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql
                    && (sql.getErrorCode() == 1213
                            || sql.getErrorCode() == 1205
                            || sql.getErrorCode() == 1020)) {
                return true;
            }
        }
        return false;
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted during lock retry backoff", interrupted);
        }
    }

    private static ApplicationException invalid(String message) {
        return new ApplicationException(new InvalidRequest(message));
    }

    private record InvalidRequest(String message) implements ErrorCode {
        @Override
        public String code() {
            return "LEDGER-INVALID-REQUEST";
        }

        @Override
        public ErrorType type() {
            return ErrorType.INVALID;
        }
    }
}

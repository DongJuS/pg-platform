package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.TransferCompletedEvent;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;
import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.readmodel.TransferReservationDraft;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import com.shinhan.pg.transfer.domain.policy.TransferIdempotencyPolicy;
import com.shinhan.pg.transfer.domain.policy.TransferStatePolicy;

import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * [MVP1-F15,F16,F17,F20] 이체 금융 쓰기의 원자성 책임자(ADR 0001). TransferAtomicWriteSkeleton의 실구현.
 *
 * <p>모든 메서드는 자체 DataSourceTransactionManager 위의 짧은 로컬 트랜잭션에서 실행되며,
 * 외부 HTTP 호출은 이 클래스 안에 없다(열린 트랜잭션 안에서 외부 호출 금지).</p>
 *
 * <ul>
 *   <li>reserveRequest: transfers insert + 전이 이력을 같은 PostgreSQL 트랜잭션에 기록한다.
 *       unique(actor_user_id, idempotency_key) 충돌은 TransferIdempotencyPolicy에 따라 409다(Redis 아님).</li>
 *   <li>completeOnceWithOutbox: 조건부 update(PROCESSING/UNKNOWN → COMPLETED)로 REST 완료와
 *       ledger.posted 소비가 같은 완료 처리를 중복 수행하지 않는다. 상태 전이·이력·transfer.completed
 *       Outbox append는 같은 트랜잭션에서 한 번만 반영된다.</li>
 *   <li>markUnknown/markFailed: 최종 상태 역전 없이 조건부로 전이한다.</li>
 * </ul>
 */
public final class TransferAtomicWriteAdapter implements TransferAtomicWritePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final OutboxStore outboxStore;
    private final TransferIdempotencyPolicy idempotencyPolicy = new TransferIdempotencyPolicy();
    private final TransferStatePolicy statePolicy = new TransferStatePolicy();

    public TransferAtomicWriteAdapter(DataSource dataSource, OutboxStore outboxStore) {
        this.jdbc = new JdbcTemplate(Objects.requireNonNull(dataSource));
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        this.outboxStore = Objects.requireNonNull(outboxStore);
    }

    /**
     * 멱등 요청 예약. RECEIVED insert + 이력 → PROCESSING 전이 + 이력을 한 트랜잭션으로 기록한다.
     * unique(actor_user_id, idempotency_key) 충돌 시 트랜잭션은 롤백되고, 커밋된 기존 행을 읽어
     * 정책상 409로 거절한다(같은 키 재요청은 신규 이체를 만들지 않는다 — WO-A6 확정).
     */
    @Override
    public TransferReservationDraft reserveRequest(RequestTransferCommand command) {
        Objects.requireNonNull(command);
        var transferId = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var requestHash = requestHash(command);
        try {
            tx.executeWithoutResult(
                    status -> {
                        jdbc.update(
                                "insert into transfers (id, actor_user_id, from_account_id,"
                                        + " to_account_id, amount, currency, idempotency_key,"
                                        + " request_hash, transaction_id, status)"
                                        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                transferId.toString(),
                                command.actorUserId().toString(),
                                command.fromAccountId().toString(),
                                command.toAccountId().toString(),
                                command.amount(),
                                command.currency(),
                                command.idempotencyKey(),
                                requestHash,
                                transactionId.toString(),
                                TransferStatePolicy.RECEIVED);
                        insertHistory(
                                transferId, null, TransferStatePolicy.RECEIVED, "REQUEST_RECEIVED");
                        jdbc.update(
                                "update transfers set status = ? where id = ? and status = ?",
                                TransferStatePolicy.PROCESSING,
                                transferId.toString(),
                                TransferStatePolicy.RECEIVED);
                        insertHistory(
                                transferId,
                                TransferStatePolicy.RECEIVED,
                                TransferStatePolicy.PROCESSING,
                                "LEDGER_POST_REQUESTED");
                    });
        } catch (DuplicateKeyException duplicate) {
            // 유니크 위반 시 Postgres는 해당 트랜잭션을 abort하므로, 롤백 후 커밋된 기존 행을 별도로 읽는다.
            var existing = findByIdempotencyKey(command.actorUserId(), command.idempotencyKey());
            if (existing.isPresent()) {
                idempotencyPolicy.validateDuplicateRequest(existing.get(), requestHash);
            }
            // 승자 트랜잭션이 아직 보이지 않는 극단적 경쟁 — 신규 이체를 만들지 않고 409로 거절한다.
            throw new TransferException(
                    TransferErrorCode.DUPLICATE_REQUEST,
                    "Duplicate transfer request for the same idempotency key");
        }
        return new TransferReservationDraft(transferId, transactionId);
    }

    /**
     * 완료의 단일 멱등 경로. REST 동기 완료와 Kafka ledger.posted 소비가 모두 여기로 수렴한다.
     * 조건부 update가 0행(이미 COMPLETED)이면 이력·Outbox를 다시 쓰지 않고 조용히 흡수한다.
     * 최종 상태 FAILED로의 역전은 INVALID_STATE로 거절한다.
     */
    @Override
    public void completeOnceWithOutbox(CompleteTransferCommand command) {
        Objects.requireNonNull(command);
        tx.executeWithoutResult(
                status -> {
                    var row =
                            lockTransfer(command.transferId())
                                    .orElseThrow(
                                            () ->
                                                    new TransferException(
                                                            TransferErrorCode.NOT_FOUND,
                                                            "Transfer not found: "
                                                                    + command.transferId()));
                    if (!row.transactionId().equals(command.transactionId())) {
                        // 식별자 조합 불일치 — 완료를 잘못된 거래에 연결하지 않는다.
                        throw new TransferException(
                                TransferErrorCode.INVALID_STATE,
                                "Transaction id does not match the transfer");
                    }
                    if (TransferStatePolicy.COMPLETED.equals(row.status())) {
                        return; // REST 완료와 ledger.posted의 중복 도달 — 멱등 흡수
                    }
                    if (statePolicy.isTerminal(row.status())) {
                        throw new TransferException(
                                TransferErrorCode.INVALID_STATE,
                                "Transfer is already in a terminal state: " + row.status());
                    }
                    statePolicy.validateTransition(row.status(), TransferStatePolicy.COMPLETED);
                    var updated =
                            jdbc.update(
                                    "update transfers set status = ?"
                                            + " where id = ? and status in (?, ?)",
                                    TransferStatePolicy.COMPLETED,
                                    command.transferId().toString(),
                                    TransferStatePolicy.PROCESSING,
                                    TransferStatePolicy.UNKNOWN);
                    if (updated == 0) {
                        return; // 동시 완료 경쟁에서 패배 — 승자가 이미 같은 완료 처리를 반영했다
                    }
                    insertHistory(
                            command.transferId(),
                            row.status(),
                            TransferStatePolicy.COMPLETED,
                            "LEDGER_POSTED");
                    // transfer.completed 발행 책임은 transfer에 있다(ADR 0001 2-B).
                    // eventId는 촉발한 ledger.posted/REST 영수증의 eventId를 이어받아
                    // 동일 사건의 재처리가 같은 이벤트 ID를 갖게 한다(없으면 신규 발급).
                    var eventId =
                            command.eventId() != null ? command.eventId() : UUID.randomUUID();
                    var event =
                            new TransferCompletedEvent(
                                    eventId,
                                    command.transferId(),
                                    command.transactionId(),
                                    command.journalId(),
                                    Instant.now());
                    outboxStore.append(
                            new OutboxRecord(
                                    UUID.randomUUID(),
                                    eventId,
                                    EventTopics.TRANSFER_COMPLETED,
                                    command.transferId().toString(),
                                    EventCodec.toBytes(event)));
                });
    }

    /** 결과 불명(타임아웃·5xx 등) — 실패로 확정하지 않고 UNKNOWN으로 둔다. 재출금 없이 재조회 대상이 된다. */
    @Override
    public void markUnknown(UUID transferId) {
        Objects.requireNonNull(transferId);
        tx.executeWithoutResult(
                status -> {
                    var row = lockTransfer(transferId);
                    if (row.isEmpty()
                            || !TransferStatePolicy.PROCESSING.equals(row.get().status())) {
                        return; // 최종 상태 덮어쓰기 금지
                    }
                    jdbc.update(
                            "update transfers set status = ? where id = ? and status = ?",
                            TransferStatePolicy.UNKNOWN,
                            transferId.toString(),
                            TransferStatePolicy.PROCESSING);
                    insertHistory(
                            transferId,
                            TransferStatePolicy.PROCESSING,
                            TransferStatePolicy.UNKNOWN,
                            "LEDGER_OUTCOME_UNKNOWN");
                });
    }

    /** 원장의 명시적 거절(422)을 최종 실패로 반영한다. 최종 상태 역전은 하지 않는다. */
    @Override
    public void markFailed(UUID transferId, String reasonCode) {
        Objects.requireNonNull(transferId);
        tx.executeWithoutResult(
                status -> {
                    var row = lockTransfer(transferId);
                    if (row.isEmpty() || statePolicy.isTerminal(row.get().status())) {
                        return;
                    }
                    statePolicy.validateTransition(row.get().status(), TransferStatePolicy.FAILED);
                    jdbc.update(
                            "update transfers set status = ? where id = ? and status in (?, ?)",
                            TransferStatePolicy.FAILED,
                            transferId.toString(),
                            TransferStatePolicy.PROCESSING,
                            TransferStatePolicy.UNKNOWN);
                    insertHistory(
                            transferId, row.get().status(), TransferStatePolicy.FAILED, reasonCode);
                });
    }

    private record LockedTransfer(UUID id, UUID transactionId, String status) {}

    private java.util.Optional<LockedTransfer> lockTransfer(UUID transferId) {
        return jdbc.query(
                        "select id, transaction_id, status from transfers where id = ? for update",
                        (row, index) ->
                                new LockedTransfer(
                                        UUID.fromString(row.getString("id")),
                                        UUID.fromString(row.getString("transaction_id")),
                                        row.getString("status")),
                        transferId.toString())
                .stream()
                .findFirst();
    }

    private java.util.Optional<com.shinhan.pg.transfer.domain.model.Transfer> findByIdempotencyKey(
            UUID actorUserId, String idempotencyKey) {
        return jdbc.query(
                        "select id, actor_user_id, from_account_id, to_account_id, amount, currency,"
                                + " idempotency_key, request_hash, transaction_id, status"
                                + " from transfers where actor_user_id = ? and idempotency_key = ?",
                        (row, index) ->
                                new com.shinhan.pg.transfer.domain.model.Transfer(
                                        UUID.fromString(row.getString("id")),
                                        UUID.fromString(row.getString("actor_user_id")),
                                        UUID.fromString(row.getString("from_account_id")),
                                        UUID.fromString(row.getString("to_account_id")),
                                        com.shinhan.pg.common.commonmoney.Money.of(
                                                row.getBigDecimal("amount"),
                                                java.util.Currency.getInstance(
                                                        row.getString("currency"))),
                                        row.getString("idempotency_key"),
                                        row.getString("request_hash"),
                                        UUID.fromString(row.getString("transaction_id")),
                                        row.getString("status")),
                        actorUserId.toString(),
                        idempotencyKey)
                .stream()
                .findFirst();
    }

    private void insertHistory(
            UUID transferId, String previousStatus, String nextStatus, String reasonCode) {
        jdbc.update(
                "insert into transfer_histories"
                        + " (id, transfer_id, previous_status, next_status, reason_code, occurred_at)"
                        + " values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(),
                transferId.toString(),
                previousStatus,
                nextStatus,
                reasonCode,
                Timestamp.from(Instant.now()));
    }

    /** 멱등키와 별개로 요청 본문의 의미를 비교하기 위한 결정적 해시(SHA-256 hex). */
    static String requestHash(RequestTransferCommand command) {
        var canonical =
                String.join(
                        "|",
                        command.fromAccountId().toString(),
                        command.toAccountId().toString(),
                        command.amount().stripTrailingZeros().toPlainString(),
                        command.currency());
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            LoggerFactory.getLogger(TransferAtomicWriteAdapter.class)
                    .error("SHA-256 unavailable", e);
            throw new IllegalStateException(e);
        }
    }
}

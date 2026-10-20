package com.shinhan.pg.ledger.application.service;

import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.GetBalanceCommand;
import com.shinhan.pg.ledger.application.command.ListJournalEntriesCommand;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.command.QueryJournalCommand;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;
import com.shinhan.pg.ledger.application.port.in.GetBalanceUseCase;
import com.shinhan.pg.ledger.application.port.in.ListJournalEntriesUseCase;
import com.shinhan.pg.ledger.application.port.in.PostJournalUseCase;
import com.shinhan.pg.ledger.application.port.in.QueryJournalUseCase;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.readmodel.JournalEntryView;
import com.shinhan.pg.ledger.application.result.GetBalanceResult;
import com.shinhan.pg.ledger.application.result.ListJournalEntriesResult;
import com.shinhan.pg.ledger.application.result.PostJournalResult;
import com.shinhan.pg.ledger.application.result.QueryJournalResult;
import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
import com.shinhan.pg.ledger.domain.error.LedgerException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

/**
 * 기능별 계약과 TODO를 모은 application 구현 자리. Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB
 * 트랜잭션 안에서 수행하지 않는다. MVP1-F11~F14는 실구현됐다. 금융 쓰기(분개·잔액·Outbox)의 원자성은 LedgerAtomicWritePort 구현
 * (LedgerAtomicWriteAdapter)의 단일 MariaDB 잠금 트랜잭션이 담당한다.
 */
public final class LedgerApplicationService
        implements PostJournalUseCase,
                ApplyInstitutionResultUseCase,
                QueryJournalUseCase,
                GetBalanceUseCase,
                ListJournalEntriesUseCase {
    /** F12 페이지 크기 계약 미정 — 보수적 기본/상한. TODO[CONTRACT]: 최종 계약 시 조정. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    private static final int MAX_PAGE_SIZE = 100;

    private final LedgerAtomicWritePort ledgerAtomicWritePort;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalRepository journalRepository;
    private final PostingRepository postingRepository;
    private final com.shinhan.pg.ledger.application.port.out.AccountQueryPort accountQueryPort;

    public LedgerApplicationService(
            LedgerAtomicWritePort ledgerAtomicWritePort,
            LedgerAccountRepository ledgerAccountRepository,
            JournalRepository journalRepository,
            PostingRepository postingRepository,
            com.shinhan.pg.ledger.application.port.out.AccountQueryPort accountQueryPort) {
        this.ledgerAtomicWritePort = java.util.Objects.requireNonNull(ledgerAtomicWritePort);
        this.ledgerAccountRepository = java.util.Objects.requireNonNull(ledgerAccountRepository);
        this.journalRepository = java.util.Objects.requireNonNull(journalRepository);
        this.postingRepository = java.util.Objects.requireNonNull(postingRepository);
        this.accountQueryPort = java.util.Objects.requireNonNull(accountQueryPort);
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F13] 분개 반영
     * API 초안: POST /internal/v1/ledger/postings
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: transactionId, posting 목록
     * 출력 근거: journalId
     * 정상 응답 후보: 200
     * 오류 후보: 409 중복(기존 journal 반환), 422 합계≠0
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 한 트랜잭션, 합계 0 검증, transactionId unique로 중복 차단
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: ledger.posted만 원장 Outbox에 저장한다. transfer.completed는 Transfer의 책임이다.
     * TODO: 계좌 ID 정렬 잠금 → 잔액/통화별 합계 0 검증 → 분개·잔액·Outbox 같은 MariaDB 트랜잭션. 외부 HTTP를 잠금 중 호출하지 않는다.
     * TODO: 원장 동일 transactionId 결과 재조회와 클라이언트 중복 이체 409는 서로 다른 계약이다. 409/기존 journal 응답의 최종 형식은 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498164b8eeca1655e74d32
     * </pre>
     */
    @Override
    public PostJournalResult postJournal(PostJournalCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        //   - 신원: X-Internal-Caller 헤더는 AuthContextFilter가 callerServiceId로 싣는다.
        //     TODO: 서비스 인증 토큰 방식은 지시서 §5-③ 미정 — 헤더 부재를 거절하지 않는다.
        //   - 입력: 아래 requireValid로 null/빈 목록/통화 코드를 검증한다(400).
        requireValid(command != null, "command is required");
        requireValid(command.transactionId() != null, "transactionId is required");
        requireValid(
                command.postings() != null && !command.postings().isEmpty(),
                "postings must not be empty");
        for (var instruction : command.postings()) {
            requireValid(
                    instruction != null
                            && instruction.accountId() != null
                            && instruction.signedAmount() != null
                            && instruction.currency() != null,
                    "posting requires accountId, signedAmount, currency");
            try {
                Currency.getInstance(instruction.currency());
            } catch (IllegalArgumentException error) {
                throw invalid("unknown currency: " + instruction.currency());
            }
        }
        // TODO 2~4: 한 트랜잭션·합계 0·transactionId unique·ledger.posted Outbox는
        // LedgerAtomicWriteAdapter가 구현한다. 이 계층은 트랜잭션을 열지 않는다.
        return ledgerAtomicWritePort.postWithPessimisticLocks(command);
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F06,MVP5-F08] Kafka fep.ack 수신. 확정/역분개는 기존 거래 ID에 대해 중복 없이 처리한다.
     * TODO: ACSC → 확정 분개, RJCT → 역분개로 고객 계좌 전액 반환 / 예외: 중복 응답은 무시
     * TODO: fep.ack 발행자·소비자와 최종 상태 갱신 책임을 정함. 수신 확인·진행 중·최종 결과를 구분하고 확정·역분개는 멱등 처리. / 예외: 미확정 응답·계속 무응답은 확인 대기. 지원 상태 코드와 최종성 정의는 보류.
     * TODO: 재시도·중복 효과 방지·로컬 트랜잭션·상태/Outbox를 연결한다. 실제 DB/통신 구현 없음.
     * </pre>
     */
    @Override
    public void applyInstitutionResult(ApplyInstitutionResultCommand command) {
        // The original transfer identity selects the hold; the local lock transaction applies at
        // most one final effect.
        ledgerAtomicWritePort.applyInstitutionResultWithPessimisticLocks(command);
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F14] 반영 결과 조회
     * API 초안: GET /internal/v1/ledger/transactions/{transactionId}
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: transactionId
     * 출력 근거: journal 또는 404
     * 정상 응답 후보: 200
     * 오류 후보: 404 미반영
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 재시도·대사용
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 조회 404만으로 이체의 최종 실패를 단정하지 않는다. 처리 중/불명과 미반영의 구분은 후속 계약이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981f79aa8dadf63136167
     * </pre>
     */
    @Override
    public QueryJournalResult queryJournal(QueryJournalCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다. (서비스 인증 토큰 §5-③ 미정)
        requireValid(
                command != null && command.transactionId() != null, "transactionId is required");
        // TODO 2: 재시도·대사용 조회. 미반영이면 TRANSACTION_UNKNOWN → 404.
        var journal =
                journalRepository
                        .findByTransactionId(command.transactionId())
                        .orElseThrow(
                                () ->
                                        new LedgerException(
                                                LedgerErrorCode.TRANSACTION_UNKNOWN,
                                                "no journal for transactionId="
                                                        + command.transactionId()));
        // TODO 3: 조회는 상태를 바꾸지 않는다.
        // TODO 4: status 명칭("POSTED")은 조회 계약 미정 — 확정 시 갱신한다.
        var eventId = journalRepository.findPostedEventId(command.transactionId()).orElse(null);
        return new QueryJournalResult(journal.transactionId(), journal.id(), eventId, "POSTED");
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F11] 잔액 조회
     * API 초안: GET /api/v1/accounts/{accountId}/balances
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: accountId
     * 출력 근거: 잔액, 기준 시각
     * 정상 응답 후보: 200
     * 오류 후보: 403
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 소유권 검사, posting 합으로 잔액 계산
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981b9b6c9fc7a3de22119
     * </pre>
     */
    @Override
    public GetBalanceResult getBalance(GetBalanceCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        //   X-User-Id는 AuthContextFilter가 actorUserId로 싣는다(없으면 401).
        requireValid(command != null && command.accountId() != null, "accountId is required");
        var verifiedAccount = requireOwnedAccount(command.actorUserId(), command.accountId());
        // 2. 소유권의 원본은 Account 서비스이며 requireOwnedAccount에서 확인한다.
        //   actorUserId와 Account의 ownerId가 다르면 403이다.
        //   잔액은 posting 합의 갱신 결과인 ledger_accounts.balance를 읽는다(스냅샷 캐시 아님).
        //   분개 이력이 없는 본인 계좌는 Account의 통화로 0원/200을 반환한다.
        //   계좌 자체가 없으면 Account 조회의 404를 유지한다.
        var account = ledgerAccountRepository.findByExternalAccountId(command.accountId());
        if (account.isEmpty()) {
            return new GetBalanceResult(
                    command.accountId(),
                    java.math.BigDecimal.ZERO,
                    verifiedAccount.currency(),
                    Instant.now());
        }
        // TODO 3: 조회는 상태를 바꾸지 않는다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        return new GetBalanceResult(
                command.accountId(),
                account.get().balance().amount(),
                account.get().balance().currency().getCurrencyCode(),
                Instant.now());
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F12] 원장 거래내역
     * API 초안: GET /api/v1/accounts/{accountId}/entries
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: from, to, cursor
     * 출력 근거: entry 목록, nextCursor
     * 정상 응답 후보: 200
     * 오류 후보: 403
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 소유권 검사, 시간 역순
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ddbaffdc6c0103bf25
     * </pre>
     */
    @Override
    public ListJournalEntriesResult listJournalEntries(ListJournalEntriesCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다. (소유권은 Account 내부 조회로 확인)
        requireValid(command != null && command.accountId() != null, "accountId is required");
        var verifiedAccount = requireOwnedAccount(command.actorUserId(), command.accountId());
        requireValid(command.size() == null || command.size() > 0, "size must be positive");
        requireValid(
                command.from() == null
                        || command.to() == null
                        || !command.from().isAfter(command.to()),
                "from must not be after to");
        // 2. F11과 같은 소유권 확인 후 시간 역순 키셋 페이지네이션을 적용한다.
        //   본인 계좌에 분개 이력이 없으면 빈 목록을 반환한다.
        var size =
                command.size() == null
                        ? DEFAULT_PAGE_SIZE
                        : Math.min(command.size(), MAX_PAGE_SIZE);
        Instant cursorPostedAt = null;
        UUID cursorJournalId = null;
        if (command.cursor() != null && !command.cursor().isBlank()) {
            var decoded = decodeCursor(command.cursor());
            cursorPostedAt = decoded.postedAt();
            cursorJournalId = decoded.journalId();
        }
        // TODO 3: 조회는 상태를 바꾸지 않는다. size+1건을 읽어 다음 페이지 유무를 판별한다.
        var rows =
                new ArrayList<>(
                        postingRepository.findEntriesByExternalAccountId(
                                command.accountId(),
                                command.from(),
                                command.to(),
                                cursorPostedAt,
                                cursorJournalId,
                                size + 1));
        String nextCursor = null;
        if (rows.size() > size) {
            rows = new ArrayList<>(rows.subList(0, size));
            nextCursor = encodeCursor(rows.get(rows.size() - 1));
        }
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        return new ListJournalEntriesResult(List.copyOf(rows), nextCursor);
    }

    private com.shinhan.pg.ledger.application.readmodel.AccountAccessSnapshot requireOwnedAccount(
            UUID actor, UUID accountId) {
        if (actor == null)
            throw new ApplicationException(
                    new SimpleErrorCode(
                            "LEDGER-UNAUTHORIZED",
                            ErrorType.UNAUTHORIZED,
                            "Authenticated user required"));
        var account = accountQueryPort.getAccount(accountId);
        if (!actor.equals(account.ownerId()))
            throw new ApplicationException(
                    new SimpleErrorCode(
                            "LEDGER-FORBIDDEN", ErrorType.FORBIDDEN, "Not the account owner"));
        return account;
    }

    /** 커서는 (postedAt, journalId) 키셋. 외부에 노출되는 불투명 값으로 base64url 인코딩한다. */
    private static String encodeCursor(JournalEntryView last) {
        var raw = last.postedAt().toString() + "|" + last.journalId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private record DecodedCursor(Instant postedAt, UUID journalId) {}

    private static DecodedCursor decodeCursor(String cursor) {
        try {
            var raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            var separator = raw.lastIndexOf('|');
            return new DecodedCursor(
                    Instant.parse(raw.substring(0, separator)),
                    UUID.fromString(raw.substring(separator + 1)));
        } catch (RuntimeException error) {
            throw invalid("invalid cursor");
        }
    }

    private static void requireValid(boolean condition, String message) {
        if (!condition) {
            throw invalid(message);
        }
    }

    private static ApplicationException invalid(String message) {
        return new ApplicationException(
                new SimpleErrorCode("LEDGER-INVALID-REQUEST", ErrorType.INVALID, message));
    }

    /** 공통 ErrorCode 계약의 ledger 구현. HTTP 상태는 PlatformExceptionHandler가 type으로 매핑한다. */
    private record SimpleErrorCode(String code, ErrorType type, String message)
            implements ErrorCode {}
}

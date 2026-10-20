package com.shinhan.pg.transfer.application.service;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.GetOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.GetTransferCommand;
import com.shinhan.pg.transfer.application.command.ListTransfersCommand;
import com.shinhan.pg.transfer.application.command.RecoverTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.GetOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.GetTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.ListTransfersUseCase;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestTransferUseCase;
import com.shinhan.pg.transfer.application.port.out.LedgerOutcomeUnknownException;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferDetailsRepository;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.port.out.TransferHistoryRepository;
import com.shinhan.pg.transfer.application.port.out.TransferQueryPort;
import com.shinhan.pg.transfer.application.port.out.TransferRepository;
import com.shinhan.pg.transfer.application.result.GetOverseasTransferResult;
import com.shinhan.pg.transfer.application.result.GetTransferResult;
import com.shinhan.pg.transfer.application.result.ListTransfersResult;
import com.shinhan.pg.transfer.application.result.RequestOverseasTransferResult;
import com.shinhan.pg.transfer.application.result.RequestTransferResult;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;
import com.shinhan.pg.transfer.domain.policy.TransferStatePolicy;

import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 기능별 계약과 TODO를 모은 application 구현 자리. Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB
 * 트랜잭션 안에서 수행하지 않는다. WO-A6: MVP1-F15~F17, F20 실구현. 원장 REST 호출은 로컬 DB 트랜잭션 밖에서 수행하고, 상태
 * 변경·이력·Outbox는 TransferAtomicWritePort의 같은 로컬 트랜잭션에 위임한다. MVP5 delegates to
 * OverseasTransferService when the explicit development gate is enabled.
 */
public final class TransferApplicationService
        implements RequestTransferUseCase,
                CompleteTransferUseCase,
                RecoverTransferUseCase,
                GetTransferUseCase,
                ListTransfersUseCase,
                RequestOverseasTransferUseCase,
                GetOverseasTransferUseCase {
    private static final Set<String> KNOWN_STATUSES =
            Set.of(
                    TransferStatePolicy.RECEIVED,
                    TransferStatePolicy.PROCESSING,
                    TransferStatePolicy.COMPLETED,
                    TransferStatePolicy.FAILED,
                    TransferStatePolicy.UNKNOWN);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final LedgerPostingPort ledgerPostingPort;
    private final OtpVerificationPort otpVerificationPort;
    private final TransferAtomicWritePort transferAtomicWritePort;
    private final TransferRepository transferRepository;
    private final TransferHistoryRepository transferHistoryRepository;
    private final OverseasTransferDetailsRepository overseasTransferDetailsRepository;
    private final TransferQueryPort transferQueryPort;
    private final com.shinhan.pg.transfer.application.port.out.AccountQueryPort accountQueryPort;
    private final TransferStatePolicy statePolicy = new TransferStatePolicy();
    private final OverseasTransferService overseas;

    public TransferApplicationService(
            LedgerPostingPort ledgerPostingPort,
            OtpVerificationPort otpVerificationPort,
            TransferAtomicWritePort transferAtomicWritePort,
            TransferRepository transferRepository,
            TransferHistoryRepository transferHistoryRepository,
            OverseasTransferDetailsRepository overseasTransferDetailsRepository,
            TransferQueryPort transferQueryPort,
            com.shinhan.pg.transfer.application.port.out.AccountQueryPort accountQueryPort) {
        this(
                ledgerPostingPort,
                otpVerificationPort,
                transferAtomicWritePort,
                transferRepository,
                transferHistoryRepository,
                overseasTransferDetailsRepository,
                transferQueryPort,
                accountQueryPort,
                null);
    }

    public TransferApplicationService(
            LedgerPostingPort ledgerPostingPort,
            OtpVerificationPort otpVerificationPort,
            TransferAtomicWritePort transferAtomicWritePort,
            TransferRepository transferRepository,
            TransferHistoryRepository transferHistoryRepository,
            OverseasTransferDetailsRepository overseasTransferDetailsRepository,
            TransferQueryPort transferQueryPort,
            com.shinhan.pg.transfer.application.port.out.AccountQueryPort accountQueryPort,
            OverseasTransferService overseas) {
        this.overseas = overseas;
        this.ledgerPostingPort = java.util.Objects.requireNonNull(ledgerPostingPort);
        this.otpVerificationPort = java.util.Objects.requireNonNull(otpVerificationPort);
        this.transferAtomicWritePort = java.util.Objects.requireNonNull(transferAtomicWritePort);
        this.transferRepository = java.util.Objects.requireNonNull(transferRepository);
        this.transferHistoryRepository =
                java.util.Objects.requireNonNull(transferHistoryRepository);
        this.overseasTransferDetailsRepository =
                java.util.Objects.requireNonNull(overseasTransferDetailsRepository);
        this.transferQueryPort = java.util.Objects.requireNonNull(transferQueryPort);
        this.accountQueryPort = java.util.Objects.requireNonNull(accountQueryPort);
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F15] 가상 이체
     * API 초안: POST /api/v1/transfers
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: Idempotency-Key 헤더, 출금·입금 계좌, 금액, otpToken
     * 출력 근거: transferId, 상태; 중복 요청은 오류
     * 오류 후보: 409 중복 요청·키 충돌, 422 잔액 부족, 401 OTP
     * TODO: 성공 200은 동기 완료의 응답 초안이다. 접수/결과불명 시 HTTP 응답 계약은 미정이며 코드로 성공을 꾸미지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ae8beff34a15fd47f1
     * </pre>
     *
     * WO-A6 실구현 순서(ADR 0001): 1. 입력 검증(인증 문맥·금액·통화 scale). otpToken은 필드 수용만 한다 — auth 검증 계약
     * 미정(§5-④)으로 검증 호출은 TODO로 남기고 가짜 검증을 하지 않는다. 2. 예약: unique(actor_user_id, idempotency_key) DB
     * 제약 위의 짧은 로컬 트랜잭션. 같은 키 재요청은 409. 3. 원장 post는 열린 DB 트랜잭션 밖에서 동일 transactionId로 1회 호출한다(재출금
     * 금지). 4. 동기 성공 → completeTransfer와 같은 멱등 완료 경로로 수렴. 타임아웃·결과 불명 → UNKNOWN 유지. 5. 원장 명시 거절(422)
     * → FAILED 확정 후 422를 호출자에게 전파.
     */
    @Override
    public RequestTransferResult requestTransfer(RequestTransferCommand command) {
        Objects.requireNonNull(command);
        validateRequest(command);
        if (command.otpToken() == null || command.otpToken().isBlank()) {
            throw new TransferException(
                    TransferErrorCode.OTP_INVALID, "OTP authorization is required");
        }
        var existing =
                transferRepository.findByIdempotencyKey(
                        command.actorUserId(), command.idempotencyKey());
        if (existing.isPresent()) {
            throw new TransferException(
                    TransferErrorCode.DUPLICATE_REQUEST,
                    "Idempotency key was already used",
                    existing.get().id());
        }
        var source = accountQueryPort.getAccount(command.fromAccountId());
        if (!command.actorUserId().equals(source.ownerId())) {
            throw new TransferException(
                    TransferErrorCode.ACCOUNT_FORBIDDEN, "Source account belongs to another user");
        }
        var target = accountQueryPort.getAccount(command.toAccountId());
        if (!"ACTIVE".equals(source.status()) || !"ACTIVE".equals(target.status())) {
            throw new TransferException(
                    TransferErrorCode.ACCOUNT_INACTIVE, "Both accounts must be active");
        }
        if (!command.currency().equals(source.currency())
                || !command.currency().equals(target.currency())) {
            throw new TransferException(
                    TransferErrorCode.INVALID_REQUEST, "Account currency mismatch");
        }
        // Consume authorization before reserving a transfer or calling Ledger. Identical retries
        // are safe.
        otpVerificationPort.verify(command);

        var reservation = transferAtomicWritePort.reserveRequest(command);

        LedgerReceipt receipt;
        try {
            var posted = ledgerPostingPort.post(reservation.transactionId(), command);
            receipt = new LedgerReceipt(posted.journalId(), posted.eventId());
        } catch (TransferException rejection) {
            // 원장의 명시적 거절(422)만 최종 실패로 확정한다.
            transferAtomicWritePort.markFailed(reservation.transferId(), "LEDGER_REJECTED");
            throw rejection;
        } catch (LedgerOutcomeUnknownException unknown) {
            // 결과 불명 — 즉시 실패 확정 금지. UNKNOWN으로 두고 recoverTransfer가 동일 transactionId로 재조회한다.
            transferAtomicWritePort.markUnknown(reservation.transferId());
            return new RequestTransferResult(
                    reservation.transferId(),
                    reservation.transactionId(),
                    TransferStatePolicy.UNKNOWN);
        }

        // 동기 완료. 이 호출과 Kafka ledger.posted 소비는 completeOnceWithOutbox의 조건부 update로 수렴한다.
        completeTransfer(
                new CompleteTransferCommand(
                        receipt.eventId(),
                        reservation.transferId(),
                        reservation.transactionId(),
                        receipt.journalId()));
        return new RequestTransferResult(
                reservation.transferId(),
                reservation.transactionId(),
                TransferStatePolicy.COMPLETED);
    }

    private record LedgerReceipt(UUID journalId, UUID eventId) {}

    private void validateRequest(RequestTransferCommand command) {
        if (command.actorUserId() == null) {
            throw new TransferException(
                    TransferErrorCode.UNAUTHENTICATED, "Authenticated subject is required");
        }
        if (command.idempotencyKey() == null
                || command.idempotencyKey().isBlank()
                || command.idempotencyKey().length() > 255) {
            throw new TransferException(
                    TransferErrorCode.INVALID_REQUEST, "Idempotency-Key is required");
        }
        if (command.fromAccountId() == null
                || command.toAccountId() == null
                || command.fromAccountId().equals(command.toAccountId())) {
            throw new TransferException(
                    TransferErrorCode.INVALID_REQUEST,
                    "From/to accounts are required and must differ");
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new TransferException(
                    TransferErrorCode.INVALID_REQUEST, "Amount must be positive");
        }
        try {
            // 통화 코드 유효성 + 통화 scale 강제(common-money: RoundingMode.UNNECESSARY).
            com.shinhan.pg.common.commonmoney.Money.of(
                    command.amount(), Currency.getInstance(command.currency()));
        } catch (IllegalArgumentException | ArithmeticException | NullPointerException error) {
            throw new TransferException(
                    TransferErrorCode.INVALID_REQUEST, "Invalid currency or amount scale");
        }
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F15,MVP1-F20] Kafka ledger.posted 반영 또는 REST 결과 확정. transfer.completed 발행 책임은 Transfer에 있다.
     * </pre>
     *
     * REST 동기 완료와 ledger.posted 소비가 모두 이 메서드로 들어오며, 중복 완료는 completeOnceWithOutbox의 조건부 update가
     * 흡수한다(멱등).
     */
    @Override
    public void completeTransfer(CompleteTransferCommand command) {
        Objects.requireNonNull(command);
        transferAtomicWritePort.completeOnceWithOutbox(command);
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F20] UNKNOWN 결과를 원장에서 조회·복구하는 재개 진입점.
     * 복구 작업은 기존 transactionId로 원장 결과 확인. 조회 404만으로 최종 실패를 단정하지 않음.
     * 복구 간격·수동 확인 전환 조건은 보류.
     * </pre>
     */
    @Override
    public void recoverTransfer(RecoverTransferCommand command) {
        Objects.requireNonNull(command);
        var status =
                transferQueryPort
                        .findStatusById(command.transferId())
                        .orElseThrow(
                                () ->
                                        new TransferException(
                                                TransferErrorCode.NOT_FOUND,
                                                "Transfer not found: " + command.transferId()));
        if (!TransferStatePolicy.UNKNOWN.equals(status)) {
            return; // 이미 수렴한 이체 — 복구 대상 아님
        }
        java.util.Optional<com.shinhan.pg.transfer.application.readmodel.LedgerPostingReceiptDraft>
                journal;
        try {
            journal = ledgerPostingPort.findJournal(command.transactionId());
        } catch (LedgerOutcomeUnknownException unknown) {
            LoggerFactory.getLogger(getClass())
                    .warn(
                            "recoverTransfer: ledger query inconclusive, stays UNKNOWN — {}",
                            command.transferId());
            return; // 일시 장애 — 확인 대기 유지
        }
        if (journal.isEmpty()) {
            // 404만으로 최종 실패를 단정하지 않는다(포트 계약). 다음 복구 주기에 다시 확인한다.
            LoggerFactory.getLogger(getClass())
                    .info(
                            "recoverTransfer: journal not posted yet, stays UNKNOWN — {}",
                            command.transferId());
            return;
        }
        var receipt = journal.get();
        completeTransfer(
                new CompleteTransferCommand(
                        receipt.eventId(),
                        command.transferId(),
                        command.transactionId(),
                        receipt.journalId()));
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F16] 이체 상태 조회
     * 오류 후보: 403, 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498126a203df4f8dd678f7
     * </pre>
     *
     * 본인 소유 이체만 조회한다(타인 403 / 없음 404). 조회는 상태를 바꾸지 않는다.
     */
    @Override
    public GetTransferResult getTransfer(GetTransferCommand command) {
        Objects.requireNonNull(command);
        if (command.actorUserId() == null) {
            throw new TransferException(
                    TransferErrorCode.UNAUTHENTICATED, "Authenticated subject is required");
        }
        var transfer =
                transferRepository
                        .findById(command.transferId())
                        .orElseThrow(
                                () ->
                                        new TransferException(
                                                TransferErrorCode.NOT_FOUND,
                                                "Transfer not found: " + command.transferId()));
        if (!transfer.actorUserId().equals(command.actorUserId())) {
            throw new TransferException(
                    TransferErrorCode.ACCOUNT_FORBIDDEN, "Transfer belongs to another user");
        }
        return new GetTransferResult(
                transfer.id(),
                transfer.transactionId(),
                transfer.status(),
                transferQueryPort.findHistories(transfer.id()));
    }

    /**
     *
     *
     * <pre>
     * [MVP1-F17] 이체 목록
     * 오류 후보: 401
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498101b674c0841cd7ad05
     * </pre>
     *
     * 본인 것만. offset 페이지네이션 — size+1건을 읽어 nextPage 유무를 판정한다.
     */
    @Override
    public ListTransfersResult listTransfers(ListTransfersCommand command) {
        Objects.requireNonNull(command);
        if (command.actorUserId() == null) {
            throw new TransferException(
                    TransferErrorCode.UNAUTHENTICATED, "Authenticated subject is required");
        }
        if (command.status() != null && !KNOWN_STATUSES.contains(command.status())) {
            throw new TransferException(
                    TransferErrorCode.INVALID_REQUEST,
                    "Unknown status filter: " + command.status());
        }
        int page = command.page() == null ? DEFAULT_PAGE : command.page();
        int size = command.size() == null ? DEFAULT_SIZE : command.size();
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new TransferException(TransferErrorCode.INVALID_REQUEST, "Invalid page or size");
        }
        var fetched =
                transferQueryPort.findSummaries(
                        command.actorUserId(),
                        command.from(),
                        command.to(),
                        command.status(),
                        page * size,
                        size + 1);
        var transfers = fetched.size() > size ? fetched.subList(0, size) : fetched;
        Integer nextPage = fetched.size() > size ? page + 1 : null;
        return new ListTransfersResult(List.copyOf(transfers), nextPage);
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F01] Development USD/no-FX/zero-fee overseas request.
     * 명시적 미구현 예외를 유지한다(가짜 성공 금지).
     * </pre>
     */
    @Override
    public RequestOverseasTransferResult requestOverseasTransfer(
            RequestOverseasTransferCommand command) {
        if (overseas == null) throw new FeatureNotImplementedException("MVP5-F01");
        return overseas.request(command);
    }

    /** [MVP5-F02] Authenticated owner lookup; status remains separate from external acceptance. */
    @Override
    public GetOverseasTransferResult getOverseasTransfer(GetOverseasTransferCommand command) {
        if (overseas == null) throw new FeatureNotImplementedException("MVP5-F02");
        return overseas.get(command);
    }
}

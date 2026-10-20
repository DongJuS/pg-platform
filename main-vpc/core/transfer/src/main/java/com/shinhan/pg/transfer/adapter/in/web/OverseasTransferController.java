package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.transfer.adapter.in.web.request.RequestOverseasTransferRequest;
import com.shinhan.pg.transfer.adapter.in.web.response.GetOverseasTransferResponse;
import com.shinhan.pg.transfer.adapter.in.web.response.RequestOverseasTransferResponse;
import com.shinhan.pg.transfer.application.command.GetOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.port.in.GetOverseasTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestOverseasTransferUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Development remittance API. The explicit MVP5 gate is required; 202 is durable acceptance, not
 * financial completion. Gateway-verified X-User-Id supplies identity.
 */
@RestController
@Profile("skeleton")
public final class OverseasTransferController {
    private final RequestOverseasTransferUseCase requestOverseasTransferUseCase;
    private final GetOverseasTransferUseCase getOverseasTransferUseCase;

    public OverseasTransferController(
            RequestOverseasTransferUseCase requestOverseasTransferUseCase,
            GetOverseasTransferUseCase getOverseasTransferUseCase) {
        this.requestOverseasTransferUseCase =
                java.util.Objects.requireNonNull(requestOverseasTransferUseCase);
        this.getOverseasTransferUseCase =
                java.util.Objects.requireNonNull(getOverseasTransferUseCase);
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F01] 해외 송금 신청
     * API 초안: POST /api/v1/transfers/overseas
     * 상태: swift-test opt-in 구현. 비활성 게이트는 501.
     * 인증/접근: 사용자 JWT + OTP / PUBLIC
     * 입력 근거: Idempotency-Key, 수취인, BIC, 계좌, 통화, 금액
     * 출력 근거: transferId, 상태(처리 중)
     * 응답: 202 transferId/uetr/status; 원장 최종 반영 전에는 완료를 뜻하지 않는다.
     * 오류 후보: 422 잔액 부족
     * Ledger의 대기 분개 로컬 트랜잭션과 Transfer의 상태/Outbox 로컬 트랜잭션은 별개다. USD/no-FX/zero-fee만 지원한다.
     * 합성 수취인·전문 원문은 관리자 private audit/log에 48시간 보존한다. 일반 사용자는 본인 마스킹 이력만 조회한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @PostMapping("/api/v1/transfers/overseas")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestOverseasTransferResponse requestOverseasTransfer(
            @RequestHeader(name = "X-User-Id", required = false) UUID actorUserId,
            @RequestBody RequestOverseasTransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // Use case validates trusted identity, account ownership, OTP and the explicit USD subset.
        var result =
                requestOverseasTransferUseCase.requestOverseasTransfer(
                        new RequestOverseasTransferCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.fromAccountId(),
                                request.recipientName(),
                                request.recipientAccount(),
                                request.bic(),
                                request.amount(),
                                request.currency(),
                                idempotencyKey,
                                request.otpToken()));
        return new RequestOverseasTransferResponse(
                result.transferId(), result.uetr(), result.status());
    }

    /**
     *
     *
     * <pre>
     * [MVP5-F02] 해외 송금 상태
     * API 초안: GET /api/v1/transfers/overseas/{transferId}
     * 상태: swift-test opt-in 구현. 비활성 게이트는 501.
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: transferId
     * 출력 근거: 상태, UETR, 거절 사유
     * 응답: 200, 본인 송금 상태.
     * 오류 후보: 403, 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @GetMapping("/api/v1/transfers/overseas/{transferId}")
    @ResponseStatus(HttpStatus.OK)
    public GetOverseasTransferResponse getOverseasTransfer(
            @RequestHeader(name = "X-User-Id", required = false) UUID actorUserId,
            @PathVariable("transferId") UUID transferId) {
        // Use case validates trusted identity, account ownership, OTP and the explicit USD subset.
        var result =
                getOverseasTransferUseCase.getOverseasTransfer(
                        new GetOverseasTransferCommand(UUID.randomUUID(), actorUserId, transferId));
        return new GetOverseasTransferResponse(
                result.transferId(), result.uetr(), result.status(), result.rejectionReason());
    }
}

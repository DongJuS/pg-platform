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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
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
     * <pre>
     * [MVP5-F01] 해외 송금 신청
     * API 초안: POST /api/v1/transfers/overseas
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT + OTP / PUBLIC
     * 입력 근거: Idempotency-Key, 수취인, BIC, 계좌, 통화, 금액
     * 출력 근거: transferId, 상태(처리 중)
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 422 잔액 부족
     * TODO: 원장 송금 대기 계정 가처리와 transfer.outbound를 같은 로컬 원자성 계약에 연결한다. 환율·수수료 계산식은 보류다.
     * TODO: 해외 계좌 정보는 합성 데이터만 사용하고 요청 원문을 로그에 남기지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @PostMapping("/api/v1/transfers/overseas")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RequestOverseasTransferResponse requestOverseasTransfer(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody RequestOverseasTransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
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
                                idempotencyKey));
        return new RequestOverseasTransferResponse(
                result.transferId(), result.uetr(), result.status());
    }

    /**
     * <pre>
     * [MVP5-F02] 해외 송금 상태
     * API 초안: GET /api/v1/transfers/overseas/{transferId}
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: transferId
     * 출력 근거: 상태, UETR, 거절 사유
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @GetMapping("/api/v1/transfers/overseas/{transferId}")
    @ResponseStatus(HttpStatus.OK)
    public GetOverseasTransferResponse getOverseasTransfer(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("transferId") UUID transferId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getOverseasTransferUseCase.getOverseasTransfer(
                        new GetOverseasTransferCommand(UUID.randomUUID(), actorUserId, transferId));
        return new GetOverseasTransferResponse(
                result.transferId(), result.uetr(), result.status(), result.rejectionReason());
    }
}

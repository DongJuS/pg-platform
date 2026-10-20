package com.shinhan.pg.payment.adapter.in.web;

import com.shinhan.pg.payment.adapter.in.web.request.ApprovePaymentRequest;
import com.shinhan.pg.payment.adapter.in.web.request.CancelPaymentRequest;
import com.shinhan.pg.payment.adapter.in.web.request.RequestRefundRequest;
import com.shinhan.pg.payment.adapter.in.web.response.ApprovePaymentResponse;
import com.shinhan.pg.payment.adapter.in.web.response.CancelPaymentResponse;
import com.shinhan.pg.payment.adapter.in.web.response.GetPaymentResponse;
import com.shinhan.pg.payment.adapter.in.web.response.ListRefundsResponse;
import com.shinhan.pg.payment.adapter.in.web.response.RequestRefundResponse;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.command.CancelPaymentCommand;
import com.shinhan.pg.payment.application.command.GetPaymentCommand;
import com.shinhan.pg.payment.application.command.ListRefundsCommand;
import com.shinhan.pg.payment.application.command.RequestRefundCommand;
import com.shinhan.pg.payment.application.port.in.ApprovePaymentUseCase;
import com.shinhan.pg.payment.application.port.in.CancelPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.GetPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.ListRefundsUseCase;
import com.shinhan.pg.payment.application.port.in.RequestRefundUseCase;

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

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다. 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다. */
@RestController
@Profile("skeleton")
public final class PaymentController {
    private final ApprovePaymentUseCase approvePaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final RequestRefundUseCase requestRefundUseCase;
    private final ListRefundsUseCase listRefundsUseCase;

    public PaymentController(
            ApprovePaymentUseCase approvePaymentUseCase,
            GetPaymentUseCase getPaymentUseCase,
            CancelPaymentUseCase cancelPaymentUseCase,
            RequestRefundUseCase requestRefundUseCase,
            ListRefundsUseCase listRefundsUseCase) {
        this.approvePaymentUseCase = java.util.Objects.requireNonNull(approvePaymentUseCase);
        this.getPaymentUseCase = java.util.Objects.requireNonNull(getPaymentUseCase);
        this.cancelPaymentUseCase = java.util.Objects.requireNonNull(cancelPaymentUseCase);
        this.requestRefundUseCase = java.util.Objects.requireNonNull(requestRefundUseCase);
        this.listRefundsUseCase = java.util.Objects.requireNonNull(listRefundsUseCase);
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F01] 모의 결제
     * API 초안: POST /api/v1/payments
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: Idempotency-Key, 주문번호, 금액
     * 출력 근거: paymentId, 상태
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 중복, 502 PG 오류
     * TODO: Payment → FEP 내부 REST → Mock PG. 직접 Mock PG를 호출하지 않는다. 접수와 최종 승인을 구분한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981c9b971efeb78107e6d
     * </pre>
     */
    @PostMapping("/api/v1/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovePaymentResponse approvePayment(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody ApprovePaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                approvePaymentUseCase.approvePayment(
                        new ApprovePaymentCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.orderReference(),
                                request.merchantId(),
                                request.amount(),
                                request.currency(),
                                idempotencyKey,
                                request.sourceAccountId(),
                                request.destinationAccountId()));
        return new ApprovePaymentResponse(result.paymentId(), result.status());
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F02] 결제 조회
     * API 초안: GET /api/v1/payments/{paymentId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: paymentId
     * 출력 근거: 상태, 금액
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981449d22d4d2771b461e
     * </pre>
     */
    @GetMapping("/api/v1/payments/{paymentId}")
    @ResponseStatus(HttpStatus.OK)
    public GetPaymentResponse getPayment(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("paymentId") UUID paymentId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getPaymentUseCase.getPayment(
                        new GetPaymentCommand(UUID.randomUUID(), actorUserId, paymentId));
        return new GetPaymentResponse(
                result.paymentId(),
                result.orderReference(),
                result.requestedAmount(),
                result.approvedAmount(),
                result.refundedAmount(),
                result.reservedAmount(),
                result.currency(),
                result.status());
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F03] 결제 취소
     * API 초안: POST /api/v1/payments/{paymentId}/cancel
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: paymentId
     * 출력 근거: 상태
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 취소 불가 상태
     * TODO: 진행 중 환불 예약과 취소가 충돌할 때의 우선순위·해제 조건은 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498145b515caf3f1cb101e
     * </pre>
     */
    @PostMapping("/api/v1/payments/{paymentId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public CancelPaymentResponse cancelPayment(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody CancelPaymentRequest request,
            @PathVariable("paymentId") UUID paymentId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                cancelPaymentUseCase.cancelPayment(
                        new CancelPaymentCommand(
                                UUID.randomUUID(), actorUserId, request.reason(), paymentId));
        return new CancelPaymentResponse(result.paymentId(), result.status());
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F04] 환불 요청
     * API 초안: POST /api/v1/payments/{paymentId}/refunds
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 금액, Idempotency-Key
     * 출력 근거: refundId, 상태
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 422 한도 초과
     * TODO: 확정 환불액과 처리 중 예약액을 같은 DB 트랜잭션으로 검사한다. 누적 환불 승인액 초과를 방지한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49815fb6b4c3b5663a8c92
     * </pre>
     */
    @PostMapping("/api/v1/payments/{paymentId}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestRefundResponse requestRefund(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody RequestRefundRequest request,
            @PathVariable("paymentId") UUID paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                requestRefundUseCase.requestRefund(
                        new RequestRefundCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.amount(),
                                request.reason(),
                                paymentId,
                                idempotencyKey));
        return new RequestRefundResponse(result.refundId(), result.paymentId(), result.status());
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F05] 환불 이력
     * API 초안: GET /api/v1/payments/{paymentId}/refunds
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: paymentId
     * 출력 근거: 환불 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498101a9cdcbd1595716c0
     * </pre>
     */
    @GetMapping("/api/v1/payments/{paymentId}/refunds")
    @ResponseStatus(HttpStatus.OK)
    public ListRefundsResponse listRefunds(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("paymentId") UUID paymentId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                listRefundsUseCase.listRefunds(
                        new ListRefundsCommand(UUID.randomUUID(), actorUserId, paymentId));
        return new ListRefundsResponse(result.refunds());
    }
}

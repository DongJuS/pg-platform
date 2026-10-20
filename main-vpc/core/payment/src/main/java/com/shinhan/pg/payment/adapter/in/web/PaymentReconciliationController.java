package com.shinhan.pg.payment.adapter.in.web;

import com.shinhan.pg.payment.adapter.in.web.request.StartReconciliationRequest;
import com.shinhan.pg.payment.adapter.in.web.response.GetReconciliationResponse;
import com.shinhan.pg.payment.adapter.in.web.response.StartReconciliationResponse;
import com.shinhan.pg.payment.application.command.GetReconciliationCommand;
import com.shinhan.pg.payment.application.command.StartReconciliationCommand;
import com.shinhan.pg.payment.application.port.in.GetReconciliationUseCase;
import com.shinhan.pg.payment.application.port.in.StartReconciliationUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다. 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다. */
@RestController
@Profile("skeleton")
public final class PaymentReconciliationController {
    private final StartReconciliationUseCase startReconciliationUseCase;
    private final GetReconciliationUseCase getReconciliationUseCase;

    public PaymentReconciliationController(
            StartReconciliationUseCase startReconciliationUseCase,
            GetReconciliationUseCase getReconciliationUseCase) {
        this.startReconciliationUseCase =
                java.util.Objects.requireNonNull(startReconciliationUseCase);
        this.getReconciliationUseCase = java.util.Objects.requireNonNull(getReconciliationUseCase);
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F08] 대사 실행
     * API 초안: POST /internal/v1/payments/reconciliation-runs
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: 대상 일자
     * 출력 근거: runId
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 실행 중
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49819596e2fb38eb2c914a
     * </pre>
     */
    @PostMapping("/internal/v1/payments/reconciliation-runs")
    @ResponseStatus(HttpStatus.OK)
    public StartReconciliationResponse startReconciliation(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @RequestBody StartReconciliationRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                startReconciliationUseCase.startReconciliation(
                        new StartReconciliationCommand(
                                UUID.randomUUID(), callerServiceId, request.businessDate()));
        return new StartReconciliationResponse(result.runId(), result.status());
    }

    /**
     *
     *
     * <pre>
     * [MVP2-F09] 대사 결과
     * API 초안: GET /internal/v1/payments/reconciliation-runs/{runId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: runId
     * 출력 근거: 불일치 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498124adadc5990b64c279
     * </pre>
     */
    @GetMapping("/internal/v1/payments/reconciliation-runs/{runId}")
    @ResponseStatus(HttpStatus.OK)
    public GetReconciliationResponse getReconciliation(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @PathVariable("runId") UUID runId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getReconciliationUseCase.getReconciliation(
                        new GetReconciliationCommand(UUID.randomUUID(), callerServiceId, runId));
        return new GetReconciliationResponse(result.runId(), result.status(), result.differences());
    }
}

package com.shinhan.pg.aml.adapter.in.web;

import com.shinhan.pg.aml.adapter.in.web.response.SubmitAmlReportResponse;
import com.shinhan.pg.aml.application.command.SubmitAmlReportCommand;
import com.shinhan.pg.aml.application.port.in.SubmitAmlReportUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * MVP6 development API. Dedicated operations caller is checked by the application service. Default
 * profile stays inactive. Enabled reads never approve, generate or transmit a report. Detailed
 * implemented contract and policy holds: SKELETON.md.
 */
@RestController
@Profile("skeleton")
public final class AmlReportController {
    private final SubmitAmlReportUseCase submitAmlReportUseCase;

    public AmlReportController(SubmitAmlReportUseCase submitAmlReportUseCase) {
        this.submitAmlReportUseCase = java.util.Objects.requireNonNull(submitAmlReportUseCase);
    }

    /**
     *
     *
     * <pre>
     * [MVP6-F03] KoFIU 보고 전송
     * API 초안: POST /internal/v1/aml/cases/{caseId}/reports
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: caseId
     * 출력 근거: 200이면 '보고 완료'
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 오류 코드면 '전송 실패', 재전송 가능
     * TODO: 사람 승인 단계를 추가하지 않는다. 자동 판정한 보고서의 생성·전송 유스케이스 진입점이다.
     * TODO: FEP 접수 성공과 KoFIU Mock 최종 접수를 구분한다. 전송 실패는 보고 확정 취소가 아니라 재전송 대상이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @PostMapping("/internal/v1/aml/cases/{caseId}/reports")
    @ResponseStatus(HttpStatus.OK)
    public SubmitAmlReportResponse submitAmlReport(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @PathVariable("caseId") UUID caseId) {
        var result =
                submitAmlReportUseCase.submitAmlReport(
                        new SubmitAmlReportCommand(UUID.randomUUID(), callerServiceId, caseId));
        return new SubmitAmlReportResponse(
                result.reportId(), result.status(), result.receiptReference());
    }
}

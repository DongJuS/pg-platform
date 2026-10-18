package com.shinhan.pg.wealthreport.adapter.in.web;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.adapter.in.web.response.RetryReportResponse;
import com.shinhan.pg.wealthreport.application.port.in.RetryReportUseCase;

import java.util.UUID;

/** 보류 API 파일 자리. Spring bean·HTTP mapping을 등록하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
// 보류: @RestController
// 보류: @Profile("skeleton")
public final class ReportOperationsController {
    private final RetryReportUseCase retryReportUseCase;

    public ReportOperationsController(RetryReportUseCase retryReportUseCase) {
        this.retryReportUseCase = java.util.Objects.requireNonNull(retryReportUseCase);
    }

    /**
     * <pre>
     * [MVP4-F04] 보고서 재처리
     * API 초안: POST /internal/v1/wealth/reports/{reportId}/retry
     * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: reportId
     * 출력 근거: 상태
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981529ed5d83553522998
     * </pre>
     */
    // 보류: @PostMapping("/internal/v1/wealth/reports/{reportId}/retry")
    public RetryReportResponse retryReport(String callerServiceId, UUID reportId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        throw new FeatureNotImplementedException("MVP4-F04 (HELD)");
    }
}

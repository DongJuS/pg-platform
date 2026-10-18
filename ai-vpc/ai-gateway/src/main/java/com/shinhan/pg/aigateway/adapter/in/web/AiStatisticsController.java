package com.shinhan.pg.aigateway.adapter.in.web;

import com.shinhan.pg.aigateway.adapter.in.web.response.GetAiSecurityStatisticsResponse;
import com.shinhan.pg.aigateway.application.port.in.GetAiSecurityStatisticsUseCase;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.time.Instant;

/** 보류 API 파일 자리. Spring bean·HTTP mapping을 등록하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
// 보류: @RestController
// 보류: @Profile("skeleton")
public final class AiStatisticsController {
    private final GetAiSecurityStatisticsUseCase getAiSecurityStatisticsUseCase;

    public AiStatisticsController(GetAiSecurityStatisticsUseCase getAiSecurityStatisticsUseCase) {
        this.getAiSecurityStatisticsUseCase =
                java.util.Objects.requireNonNull(getAiSecurityStatisticsUseCase);
    }

    /**
     * <pre>
     * [MVP3-F04] AI 차단 통계 조회
     * API 초안: GET /internal/v1/ai/security-statistics
     * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: 기간, 호출 서비스, 차단 사유
     * 출력 근거: 건수·사유별 집계
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 조회 기간 제한·보존 기간은 Q07과 함께 검토.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    // 보류: @GetMapping("/internal/v1/ai/security-statistics")
    public GetAiSecurityStatisticsResponse getAiSecurityStatistics(
            String callerServiceId, Instant from, Instant to, String serviceId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        throw new FeatureNotImplementedException("MVP3-F04 (HELD)");
    }
}

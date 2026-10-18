package com.shinhan.pg.aigateway.application.port.in;

import com.shinhan.pg.aigateway.application.command.GetAiSecurityStatisticsCommand;
import com.shinhan.pg.aigateway.application.result.GetAiSecurityStatisticsResult;

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
public interface GetAiSecurityStatisticsUseCase {
    GetAiSecurityStatisticsResult getAiSecurityStatistics(GetAiSecurityStatisticsCommand command);
}

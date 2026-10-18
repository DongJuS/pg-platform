package com.shinhan.pg.aigateway.application.deferred;

/**
 * <pre>
 * [MVP3-F03] 보류 상태를 보존하는 파일 자리.
 * 서비스별 토큰 예산·사용량 쿼터는 보류. 기본 AI 중계·감사 기록과 구분한다.
 * TODO: 사용자 확정 전 API annotation·Spring bean·동작을 추가하지 않는다.
 * </pre>
 */
public final class DeferredAiUsagePolicyPlan {
    private DeferredAiUsagePolicyPlan() {}
}

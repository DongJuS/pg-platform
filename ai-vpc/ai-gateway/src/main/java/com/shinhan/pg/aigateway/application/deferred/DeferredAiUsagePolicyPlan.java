package com.shinhan.pg.aigateway.application.deferred;

/**
 * <pre>
 * [MVP3-F03] 보류 상태를 보존하는 파일 자리.
 * 서비스별 토큰 예산·사용량 쿼터는 보류. 전송 준비의 서비스 인증·입력 byte 상한·감사 기록과 구분한다. 이번 byte 제한은 모델 토큰 예산이 아니다.
 * TODO: 사용자 확정 전 API annotation·Spring bean·동작을 추가하지 않는다.
 * </pre>
 */
public final class DeferredAiUsagePolicyPlan {
    private DeferredAiUsagePolicyPlan() {}
}

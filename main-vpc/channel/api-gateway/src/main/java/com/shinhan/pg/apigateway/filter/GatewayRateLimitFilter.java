package com.shinhan.pg.apigateway.filter;

/**
 * <pre>
 * TODO: Redis의 원자적 카운터/TTL로 호출자·경로별 제한. 429 계약과 저장소 실패 모드는 미정이다.
 * TODO: 구현·검증 후 GlobalFilter bean으로 조립한다. 현재 등록되지 않은 스켈레톤이다.
 * </pre>
 */
public final class GatewayRateLimitFilter {
    private GatewayRateLimitFilter() {}
}

package com.shinhan.pg.apigateway.config;

/**
 * <pre>
 * API 라우팅 구현 자리. 실제 RouteLocator bean은 아직 등록하지 않는다.
 * TODO: 외부 /api/v1/auth/**, /api/v1/users/me → Auth
 * TODO: /api/v1/accounts/{id}/balances, /entries → Ledger를 광범위 Account route보다 먼저 매칭
 * TODO: /api/v1/accounts/** → Account, /api/v1/transfers/** → Transfer, /api/v1/payments/** → Payment
 * TODO: /api/v1/wealth/reports/** → Wealth Report, 동기 생성 시간 예산 연결
 * TODO: /internal/**, /ops/**, /actuator/**, 내부 계좌 상태 변경은 외부 catch-all route에서 제외
 * TODO: 서비스 이름/주소·JWT 검증·rate limit·trace 전파를 조립하고 health 외 기본 차단을 유지
 * 보류 운영 API를 라우팅 확정으로 바꾸지 않는다.
 * </pre>
 */
public final class GatewayRoutesConfiguration {
    private GatewayRoutesConfiguration() {}
}

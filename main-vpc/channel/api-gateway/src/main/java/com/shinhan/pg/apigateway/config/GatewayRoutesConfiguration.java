package com.shinhan.pg.apigateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <pre>
 * [MVP2-F10] 헬스 체크, [MVP2-F11] 메트릭 수집, [CMN-F02] 망분리·접근 통제 — 라우팅 조립 구현 자리.
 * 실제 RouteLocator bean은 아직 등록하지 않는다.
 * [MVP2-F10] 헬스 체크: 게이트웨이 자체 /actuator/health는 기존 설정이 제공하며 중복 route를 만들지 않는다.
 *   TODO: 하위 서비스 상태 통합 노출 여부는 보류 API MVP2-F13과 함께 후속 확정한다. Figma 프레임 확인 필요.
 * [MVP2-F11] 메트릭 수집: Prometheus 노출은 기존 platform-defaults 설정이 제공한다.
 *   TODO: 라우팅과 연결할 메트릭 경계(요청 수·지연·오류율 라벨)와 치부 방식·카드널리티 한도는 업무 구현 단계에서 확정한다. Figma 프레임 확인 필요.
 * [CMN-F02] 망분리·접근 통제 — 세부 필터 책임: GatewayJwtFilter(신원 검증), GatewayRateLimitFilter(호출 제한), InternalPathBlockFilter(내부 경로 차단).
 * TODO: 외부 /api/v1/auth/**, /api/v1/users/me → Auth
 * TODO: /api/v1/accounts/{id}/balances, /entries → Ledger를 광범위 Account route보다 먼저 매칭
 * TODO: /api/v1/accounts/** → Account, /api/v1/transfers/** → Transfer, /api/v1/payments/** → Payment
 * TODO: /api/v1/wealth/reports/** → Wealth Report, 동기 생성 시간 예산 연결
 * TODO: /internal/**, /ops/**, /actuator/**, 내부 계좌 상태 변경은 외부 catch-all route에서 제외
 * TODO: 서비스 이름/주소·JWT 검증·rate limit·trace 전파를 조립하고 health 외 기본 차단을 유지
 * 보류 운영 API를 라우팅 확정으로 바꾸지 않는다.
 * </pre>
 *
 * 구현 상태(WO-A2): 위 TODO 중 MVP1 범위(auth/ledger 조회/account/transfer)는 아래
 * {@link #gatewayBusinessRoutes}에 실구현됐다. 대상 URI는 기존 `pg.clients.<service>.base-url`
 * 계약({@link GatewayClientProperties})에서 해소한다. payment/wealth 등 나머지 TODO는 보류 유지.
 * `/internal/**`·`/ops/**`·`/actuator/**`(health 제외)는 여기에 route를 등록하지 않고
 * {@link GatewaySecurityConfiguration}의 denyAll로 외부 차단을 유지한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayClientProperties.class)
public class GatewayRoutesConfiguration {

    /**
     * CMN-F02 업무 라우트. order가 매칭 우선순위다(낮을수록 먼저 매칭):
     * ledger의 balances/entries 조회는 account 광범위 라우트보다 반드시 먼저 선언한다.
     * 인증 정책(permitAll/authenticated)은 {@link GatewaySecurityConfiguration}의 책임이다.
     */
    @Bean
    RouteLocator gatewayBusinessRoutes(RouteLocatorBuilder builder, GatewayClientProperties clients) {
        return builder.routes()
                .route("auth", r -> r.order(1)
                        .path("/api/v1/auth/**", "/api/v1/users/me")
                        .uri(clients.auth().baseUrl()))
                .route("ledger-account-views", r -> r.order(2)
                        .path("/api/v1/accounts/{id}/balances", "/api/v1/accounts/{id}/entries")
                        .uri(clients.ledger().baseUrl()))
                .route("account", r -> r.order(3)
                        .path("/api/v1/accounts/**")
                        .uri(clients.account().baseUrl()))
                .route("transfer", r -> r.order(4)
                        .path("/api/v1/transfers/**")
                        .uri(clients.transfer().baseUrl()))
                .build();
    }
}

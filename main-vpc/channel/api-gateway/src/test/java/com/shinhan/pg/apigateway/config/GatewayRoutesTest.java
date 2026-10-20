package com.shinhan.pg.apigateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * CMN-F02 라우팅 계약 고정(WO-A2): - 선언 순서: auth → ledger(balances/entries) → account → transfer
 * (balances/entries는 광범위 accounts 라우트보다 먼저 매칭되어야 한다) - 대상 URI는 pg.clients.<service>.base-url 계약에서
 * 해소된다 - /internal/**, /actuator/**(health 외) 등은 어떤 라우트에도 매칭되지 않는다 standalone 방식 — Spring 컨텍스트 기동
 * 없음(테스트용 GenericApplicationContext에 PathRoutePredicateFactory만 등록; RouteLocatorBuilder가 predicate
 * factory를 bean 조회한다).
 */
class GatewayRoutesTest {
    private static final GatewayClientProperties CLIENTS =
            new GatewayClientProperties(
                    new GatewayClientProperties.ServiceEndpoint(URI.create("http://auth:8081")),
                    new GatewayClientProperties.ServiceEndpoint(URI.create("http://account:8082")),
                    new GatewayClientProperties.ServiceEndpoint(URI.create("http://ledger:8083")),
                    new GatewayClientProperties.ServiceEndpoint(URI.create("http://transfer:8084")),
                    new GatewayClientProperties.ServiceEndpoint(URI.create("http://payment:8085")));

    private static List<Route> routes() {
        var context = new GenericApplicationContext();
        context.registerBean(
                PathRoutePredicateFactory.class, () -> new PathRoutePredicateFactory());
        context.refresh();
        try {
            var locator =
                    new GatewayRoutesConfiguration()
                            .gatewayBusinessRoutes(new RouteLocatorBuilder(context), CLIENTS);
            return locator.getRoutes().collectList().block(Duration.ofSeconds(5));
        } finally {
            context.close();
        }
    }

    private static boolean matches(Route route, String path) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        return Boolean.TRUE.equals(
                Mono.from(route.getPredicate().apply(exchange)).block(Duration.ofSeconds(5)));
    }

    private static Route route(List<Route> routes, String id) {
        return routes.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("route 없음: " + id));
    }

    @Test
    void routesAreDeclaredInContractOrder() {
        var ids = routes().stream().map(Route::getId).toList();
        assertEquals(
                List.of("auth", "ledger-account-views", "account", "transfer", "payment"),
                ids,
                "라우트는 auth → ledger(balances/entries) → account → transfer 순서여야 한다");
    }

    @Test
    void ledgerRouteWinsOverBroadAccountRouteForBalancesAndEntries() {
        var routes = routes();
        var ledger = route(routes, "ledger-account-views");
        assertTrue(matches(ledger, "/api/v1/accounts/acc-1/balances"));
        assertTrue(matches(ledger, "/api/v1/accounts/acc-1/entries"));
        // 광범위 account 라우트도 이 경로에 매칭되므로, ledger가 먼저 선언된 순서가 실효 순서를 보장한다
        assertTrue(
                matches(route(routes, "account"), "/api/v1/accounts/acc-1/balances"),
                "account 광범위 라우트도 매칭 가능하므로 선언 순서가 결정적이어야 한다");
        assertTrue(
                routes.indexOf(ledger) < routes.indexOf(route(routes, "account")),
                "ledger 라우트는 account 라우트보다 먼저 선언되어야 한다");
    }

    @Test
    void routePredicatesMatchContractPaths() {
        var routes = routes();
        assertTrue(matches(route(routes, "auth"), "/api/v1/auth/login"));
        assertTrue(matches(route(routes, "auth"), "/api/v1/users/me"));
        assertFalse(
                matches(route(routes, "auth"), "/api/v1/accounts"), "auth 라우트가 업무 경로를 삼키면 안 된다");
        assertTrue(matches(route(routes, "account"), "/api/v1/accounts/acc-1"));
        assertTrue(matches(route(routes, "transfer"), "/api/v1/transfers/tx-1"));
        assertFalse(matches(route(routes, "transfer"), "/api/v1/accounts/acc-1"));
    }

    @Test
    void routeUrisResolveFromPgClientsProperties() {
        var routes = routes();
        assertEquals(URI.create("http://auth:8081"), route(routes, "auth").getUri());
        assertEquals(
                URI.create("http://ledger:8083"), route(routes, "ledger-account-views").getUri());
        assertEquals(URI.create("http://account:8082"), route(routes, "account").getUri());
        assertEquals(URI.create("http://transfer:8084"), route(routes, "transfer").getUri());
    }

    @Test
    void internalAndCatchAllPathsMatchNoRoute() {
        var routes = routes();
        for (var path :
                List.of(
                        "/internal/v1/ledger/postings",
                        "/actuator/prometheus",
                        "/ops/status",
                        "/internal/v1/payments/pg/webhooks",
                        "/anything-else")) {
            assertTrue(
                    routes.stream().noneMatch(r -> matches(r, path)),
                    "라우트 없이 denyAll로 차단되어야 하는 경로가 매칭됐다: " + path);
        }
    }
}

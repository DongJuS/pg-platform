package com.shinhan.pg.apigateway.config;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * CMN-F02 계약 고정(WO-A2 신규 계약):
 * - /actuator/health/** → permitAll
 * - /api/v1/auth/** → permitAll
 * - 그 외 /api/v1/** → JWT 인증 필요: 토큰 없음/무효 → 401 JSON, 유효 → 통과
 * - /internal/** 등 비업무 경로 → 익명 401 / 인증됨 403, JSON ErrorResponse
 * standalone(WebFilterChainProxy + MockServerWebExchange) 방식 — Spring 컨텍스트 기동 없음.
 * 리소스서버 decoder는 stub으로 주입한다.
 */
class GatewaySecurityConfigurationTest {
    /** security chain을 통과하면 도달하는 종단 chain(200 OK) */
    private static final WebFilterChain DOWNSTREAM=exchange -> {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        return exchange.getResponse().setComplete();
    };
    private static final Jwt VALID_JWT=Jwt.withTokenValue("token")
        .header("alg","RS256")
        .subject("user-1")
        .issuer("pg-platform-auth")
        .audience(List.of("pg-platform"))
        .issuedAt(Instant.now().minusSeconds(10))
        .expiresAt(Instant.now().plusSeconds(300))
        .build();
    private static final ReactiveJwtDecoder VALID_DECODER=token -> Mono.just(VALID_JWT);
    private static final ReactiveJwtDecoder FAILING_DECODER=token -> Mono.error(new BadJwtException("invalid"));
    private static ObjectProvider<ReactiveJwtDecoder> provider(ReactiveJwtDecoder decoder) {
        return new ObjectProvider<>() {
            @Override public ReactiveJwtDecoder getObject(Object... args) { return decoder; }
            @Override public ReactiveJwtDecoder getIfAvailable() { return decoder; }
            @Override public ReactiveJwtDecoder getIfUnique() { return decoder; }
            @Override public ReactiveJwtDecoder getObject() { return decoder; }
        };
    }
    private WebFilterChainProxy proxy(ReactiveJwtDecoder decoder) {
        SecurityWebFilterChain security=new GatewaySecurityConfiguration()
            .security(ServerHttpSecurity.http(),new ObjectMapper(),provider(decoder));
        return new WebFilterChainProxy(security);
    }
    private static MockServerWebExchange run(WebFilterChainProxy proxy,MockServerHttpRequest.BaseBuilder<?> request) {
        var exchange=MockServerWebExchange.from(request);
        proxy.filter(exchange,DOWNSTREAM).block(Duration.ofSeconds(5));
        return exchange;
    }
    private static void assertJsonError(MockServerWebExchange exchange,HttpStatus status) {
        var response=exchange.getResponse();
        assertEquals(status,response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON,response.getHeaders().getContentType());
        var body=response.getBodyAsString().block(Duration.ofSeconds(5));
        assertTrue(body!=null&&body.contains("\"code\":\"COMMON-HTTP-"+status.value()+"\""),
            "차단 응답 본문의 code는 COMMON-HTTP-"+status.value()+"이어야 한다: "+body);
    }
    @Test void actuatorHealthIsPermitted() {
        var exchange=run(proxy(VALID_DECODER),MockServerHttpRequest.get("/actuator/health"));
        assertEquals(HttpStatus.OK,exchange.getResponse().getStatusCode(),
            "health 경로는 permitAll이어서 종단 chain(200)에 도달해야 한다");
    }
    @Test void actuatorHealthSubPathIsPermitted() {
        var exchange=run(proxy(VALID_DECODER),MockServerHttpRequest.get("/actuator/health/readiness"));
        assertEquals(HttpStatus.OK,exchange.getResponse().getStatusCode(),
            "/actuator/health/** 하위 경로도 permitAll이어야 한다");
    }
    @Test void anonymousAuthRouteIsPermitted() {
        var exchange=run(proxy(VALID_DECODER),MockServerHttpRequest.get("/api/v1/auth/login"));
        assertEquals(HttpStatus.OK,exchange.getResponse().getStatusCode(),
            "/api/v1/auth/**는 토큰 없이도 permitAll이어야 한다");
    }
    @Test void anonymousBusinessRouteIsDeniedWith401Json() {
        var exchange=run(proxy(VALID_DECODER),MockServerHttpRequest.get("/api/v1/accounts"));
        assertJsonError(exchange,HttpStatus.UNAUTHORIZED);
    }
    @Test void invalidBearerTokenIsDeniedWith401Json() {
        // 검증 실패(서명/만료/issuer/audience 불일치) 토큰 → 리소스서버 entryPoint도 동일 JSON 계약이어야 한다
        var exchange=run(proxy(FAILING_DECODER),
            MockServerHttpRequest.get("/api/v1/accounts").header("Authorization","Bearer bad-token"));
        assertJsonError(exchange,HttpStatus.UNAUTHORIZED);
    }
    @Test void validBearerTokenReachesDownstream() {
        var exchange=run(proxy(VALID_DECODER),
            MockServerHttpRequest.get("/api/v1/accounts").header("Authorization","Bearer good-token"));
        assertEquals(HttpStatus.OK,exchange.getResponse().getStatusCode(),
            "유효 JWT면 /api/v1/**는 인증 통과 후 종단 chain(200)에 도달해야 한다");
    }
    @Test void validTokenToInternalPathIsDeniedWith403Json() {
        var exchange=run(proxy(VALID_DECODER),
            MockServerHttpRequest.get("/internal/v1/ledger/postings").header("Authorization","Bearer good-token"));
        assertJsonError(exchange,HttpStatus.FORBIDDEN);
    }
    @Test void anonymousInternalPathIsDeniedWith401Json() {
        var exchange=run(proxy(VALID_DECODER),MockServerHttpRequest.get("/internal/v1/ledger/postings"));
        assertJsonError(exchange,HttpStatus.UNAUTHORIZED);
    }
    @Test void anonymousCatchAllPathIsDeniedWith401Json() {
        var exchange=run(proxy(VALID_DECODER),MockServerHttpRequest.get("/anything-else"));
        assertJsonError(exchange,HttpStatus.UNAUTHORIZED);
    }
}

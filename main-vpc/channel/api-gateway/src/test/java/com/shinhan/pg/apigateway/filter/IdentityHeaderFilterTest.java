package com.shinhan.pg.apigateway.filter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
/**
 * CMN-F02 인증 문맥 계약 고정(WO-A2):
 * - 외부가 보낸 X-User-Id / X-Internal-Caller는 항상 제거된다(스푸핑 차단)
 * - 검증된 JWT가 있으면 sub가 X-User-Id로 downstream에 전달된다
 * standalone(MockServerWebExchange) 방식 — Spring 컨텍스트 기동 없음.
 */
class IdentityHeaderFilterTest {
    private final IdentityHeaderFilter filter=new IdentityHeaderFilter();
    private static final Jwt JWT=Jwt.withTokenValue("token")
        .header("alg","RS256")
        .subject("user-1")
        .issuer("pg-platform-auth")
        .audience(List.of("pg-platform"))
        .issuedAt(Instant.now().minusSeconds(10))
        .expiresAt(Instant.now().plusSeconds(300))
        .build();
    private static AtomicReference<HttpHeaders> capture() { return new AtomicReference<>(); }
    private static GatewayFilterChain capturing(AtomicReference<HttpHeaders> seen) {
        return exchange -> { seen.set(exchange.getRequest().getHeaders()); return Mono.empty(); };
    }
    @Test void externalIdentityHeadersAreAlwaysScrubbedWithoutAuthentication() {
        var exchange=MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/accounts")
            .header("X-User-Id","attacker")
            .header("X-Internal-Caller","attacker"));
        var seen=capture();
        filter.filter(exchange,capturing(seen)).block(Duration.ofSeconds(5));
        assertFalse(seen.get().containsKey("X-User-Id"),
            "익명 요청의 외부 X-User-Id는 제거되어야 한다");
        assertFalse(seen.get().containsKey("X-Internal-Caller"),
            "외부 X-Internal-Caller는 제거되어야 한다");
    }
    @Test void verifiedJwtSubjectIsForwardedAsUserIdAndExternalHeadersScrubbed() {
        var exchange=MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/accounts")
            .header("X-User-Id","attacker")
            .header("X-Internal-Caller","attacker"));
        var seen=capture();
        filter.filter(exchange,capturing(seen))
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(new JwtAuthenticationToken(JWT)))
            .block(Duration.ofSeconds(5));
        assertEquals("user-1",seen.get().getFirst("X-User-Id"),
            "검증된 JWT의 sub가 X-User-Id로 전달되어야 한다(외부 값 덮어쓰기)");
        assertFalse(seen.get().containsKey("X-Internal-Caller"),
            "gateway는 X-Internal-Caller를 생성하지 않고 외부 값도 제거한다");
    }
    @Test void otherHeadersArePreserved() {
        var exchange=MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/accounts")
            .header("X-Trace-Id","abc")
            .header("X-User-Id","attacker"));
        var seen=capture();
        filter.filter(exchange,capturing(seen)).block(Duration.ofSeconds(5));
        assertEquals("abc",seen.get().getFirst("X-Trace-Id"),
            "신원 헤더 외 헤더는 유지되어야 한다");
    }
}

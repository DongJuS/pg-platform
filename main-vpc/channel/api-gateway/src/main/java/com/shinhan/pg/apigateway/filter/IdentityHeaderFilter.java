package com.shinhan.pg.apigateway.filter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
/**
 * CMN-F02 인증 문맥 계약(지시서 §3):
 * - 외부 요청이 보낸 X-User-Id / X-Internal-Caller 헤더는 항상 제거한다(신원 스푸핑 차단).
 * - 리소스서버가 검증한 JWT가 있으면 그 sub를 X-User-Id로 downstream에 전달한다.
 * - X-Internal-Caller는 gateway가 생성하지 않는다(서비스 간 호출만 내부망에서 표식).
 */
@Component
public final class IdentityHeaderFilter implements GlobalFilter, Ordered {
    public static final String USER_ID_HEADER="X-User-Id";
    public static final String INTERNAL_CALLER_HEADER="X-Internal-Caller";
    @Override public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var scrubbed=exchange.getRequest().mutate()
            .headers(headers -> {
                headers.remove(USER_ID_HEADER);
                headers.remove(INTERNAL_CALLER_HEADER);
                headers.remove("X-Internal-Service-Token");
            })
            .build();
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(context -> Mono.justOrEmpty(context.getAuthentication()))
            .map(authentication -> authentication.getPrincipal())
            .filter(Jwt.class::isInstance)
            .map(Jwt.class::cast)
            .map(jwt -> scrubbed.mutate().header(USER_ID_HEADER,jwt.getSubject()).build())
            .defaultIfEmpty(scrubbed)
            .flatMap(request -> chain.filter(exchange.mutate().request(request).build()));
    }
    @Override public int getOrder() { return -80; }
}

package com.shinhan.pg.apigateway.filter;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Component
public final class RateLimitFilter implements GlobalFilter, Ordered {
    @Override public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // TODO: Redis-backed atomic token bucket, key policy and 429 response.
        return chain.filter(exchange);
    }
    @Override public int getOrder() { return -90; }
}

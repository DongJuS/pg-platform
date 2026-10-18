package com.shinhan.pg.apigateway.filter;
import io.micrometer.context.ContextRegistry;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
@Component
@Order(Ordered.HIGHEST_PRECEDENCE+1)
public final class GatewayTraceFilter implements WebFilter {
    private static final String KEY="platform.traceId";
    private final ObjectProvider<Tracer> provider;
    public GatewayTraceFilter(ObjectProvider<Tracer> provider) { this.provider=provider; }
    @PostConstruct public void register() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(KEY,() -> MDC.get("traceId"),value -> MDC.put("traceId",value),() -> MDC.remove("traceId"));
    }
    @PreDestroy public void unregister() { ContextRegistry.getInstance().removeThreadLocalAccessor(KEY); }
    @Override public Mono<Void> filter(ServerWebExchange exchange,WebFilterChain chain) {
        return Mono.defer(() -> {
            var tracer=provider.getIfAvailable();
            var span=tracer==null ? null : tracer.currentSpan();
            var supplied=exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
            var trace=span!=null ? span.context().traceId() : supplied!=null && supplied.matches("[0-9a-f]{32}") ? supplied : UUID.randomUUID().toString().replace("-","");
            exchange.getAttributes().put("traceId",trace);
            exchange.getResponse().getHeaders().set("X-Trace-Id",trace);
            return chain.filter(exchange).contextWrite(context -> context.put(KEY,trace));
        });
    }
}

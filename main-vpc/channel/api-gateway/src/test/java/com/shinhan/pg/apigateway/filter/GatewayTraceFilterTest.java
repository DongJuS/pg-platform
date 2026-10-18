package com.shinhan.pg.apigateway.filter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GatewayTraceFilterTest {
    @Test void traceSurvivesThreadHopAndDoesNotLeak() {
        @SuppressWarnings("unchecked") var provider=(ObjectProvider<Tracer>)mock(ObjectProvider.class);
        var filter=new GatewayTraceFilter(provider); filter.register(); Hooks.enableAutomaticContextPropagation();
        try {
            var trace="0123456789abcdef0123456789abcdef";
            var exchange=MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").header("X-Trace-Id",trace));
            var observed=new AtomicReference<String>();
            filter.filter(exchange,e -> Mono.<Void>fromRunnable(() -> observed.set(MDC.get("traceId"))).subscribeOn(Schedulers.boundedElastic())).block(Duration.ofSeconds(5));
            assertEquals(trace,exchange.getResponse().getHeaders().getFirst("X-Trace-Id"));
            assertEquals(trace,observed.get()); assertNull(MDC.get("traceId"));
        } finally { filter.unregister(); Hooks.disableAutomaticContextPropagation(); }
    }
}

package com.shinhan.pg.apigateway.config;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Duration;

/**
 * Service DNS may change on restart. Do not retain Docker/Kubernetes addresses for a long upstream
 * TTL. This changes address resolution only, never retries a financial HTTP mutation.
 */
@Configuration(proxyBeanMethods = false)
public class GatewayServiceDnsConfiguration {
    @Bean
    public HttpClientCustomizer serviceDnsCache(Environment env) {
        long seconds = env.getProperty("pg.gateway.service-dns-ttl-seconds", Long.class, 5L);
        if (seconds < 1 || seconds > 60)
            throw new IllegalArgumentException("Service DNS cache TTL must be 1..60 seconds");
        return client ->
                client.resolver(
                        spec ->
                                spec.cacheMaxTimeToLive(Duration.ofSeconds(seconds))
                                        .cacheMinTimeToLive(Duration.ZERO)
                                        .cacheNegativeTimeToLive(Duration.ZERO));
    }
}

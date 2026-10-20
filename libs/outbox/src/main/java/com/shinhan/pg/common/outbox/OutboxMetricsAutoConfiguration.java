package com.shinhan.pg.common.outbox;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Backlog visibility remains active even when the publisher is deliberately disabled. */
@AutoConfiguration(
        afterName = {
            "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
            "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration"
        })
@ConditionalOnBean({OutboxStore.class, JdbcTemplate.class, MeterRegistry.class})
@ConditionalOnProperty(name = "outbox.metrics.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxMetricsAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    OutboxBacklogMetrics outboxBacklogMetrics(JdbcTemplate jdbc, MeterRegistry registry) {
        return new OutboxBacklogMetrics(jdbc, registry, Clock.systemUTC());
    }
}

package com.shinhan.pg.common.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class OutboxMetricsAutoConfigurationTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(OutboxMetricsAutoConfiguration.class))
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class));

    @Test
    void disabledPublisherStillExposesItsBacklog() {
        runner.withBean(OutboxStore.class, () -> mock(OutboxStore.class))
                .withPropertyValues("outbox.enabled=false")
                .run(
                        context ->
                                assertEquals(
                                        1,
                                        context.getBeansOfType(OutboxBacklogMetrics.class).size()));
    }

    @Test
    void metricsCanBeExplicitlyDisabled() {
        runner.withBean(OutboxStore.class, () -> mock(OutboxStore.class))
                .withPropertyValues("outbox.metrics.enabled=false")
                .run(
                        context ->
                                assertEquals(
                                        0,
                                        context.getBeansOfType(OutboxBacklogMetrics.class).size()));
    }

    @Test
    void serviceWithoutOutboxStoreNeverQueriesAnOutboxTable() {
        runner.run(
                context ->
                        assertEquals(0, context.getBeansOfType(OutboxBacklogMetrics.class).size()));
    }
}

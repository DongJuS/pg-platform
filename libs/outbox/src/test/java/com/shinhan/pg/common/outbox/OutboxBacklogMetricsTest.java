package com.shinhan.pg.common.outbox;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class OutboxBacklogMetricsTest {
    @Test
    @SuppressWarnings("unchecked")
    void unavailableDatabaseIsUnknownRatherThanHealthyZero() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), any(RowMapper.class)))
                .thenThrow(new DataAccessResourceFailureException("synthetic outage"));
        var registry = new SimpleMeterRegistry();
        try {
            var metrics =
                    new OutboxBacklogMetrics(
                            jdbc,
                            registry,
                            Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC));
            assertTrue(Double.isNaN(registry.get("pg.outbox.unpublished.events").gauge().value()));
            metrics.refresh();
            assertTrue(Double.isNaN(registry.get("pg.outbox.unpublished.events").gauge().value()));
            assertTrue(
                    Double.isNaN(
                            registry.get("pg.outbox.oldest.unpublished.age.seconds")
                                    .gauge()
                                    .value()));
            org.junit.jupiter.api.Assertions.assertEquals(
                    0, registry.get("pg.outbox.metrics.sample.success").gauge().value());
        } finally {
            registry.close();
        }
    }
}

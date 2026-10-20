package com.shinhan.pg.common.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * [MVP2-F12] Samples only the owning service's outbox. Scrapes never perform database I/O.
 * Claimed/retryable rows remain unpublished until broker acknowledgement is persisted. No
 * transaction IDs, payloads or topic-derived labels are exposed.
 */
public final class OutboxBacklogMetrics {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private volatile Snapshot snapshot;

    public OutboxBacklogMetrics(JdbcTemplate jdbc, MeterRegistry registry, Clock clock) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.clock = Objects.requireNonNull(clock);
        Gauge.builder("pg.outbox.unpublished.events", this, OutboxBacklogMetrics::unpublished)
                .description("Unpublished events, including leased and retryable events")
                .register(registry);
        Gauge.builder(
                        "pg.outbox.oldest.unpublished.age.seconds",
                        this,
                        OutboxBacklogMetrics::oldestAge)
                .description("Age of the oldest unpublished event")
                .baseUnit("seconds")
                .register(registry);
        Gauge.builder(
                        "pg.outbox.metrics.sample.success",
                        this,
                        metrics -> metrics.snapshot == null ? 0 : 1)
                .description("One when the last local outbox database sample succeeded")
                .register(registry);
    }

    @Scheduled(fixedDelayString = "${outbox.metrics.poll-delay-ms:15000}")
    public void refresh() {
        try {
            snapshot =
                    jdbc.queryForObject(
                            "select count(*) as pending_count, min(created_at) as oldest_created_at"
                                    + " from outbox_events where published_at is null",
                            (row, index) -> {
                                var oldest = row.getTimestamp("oldest_created_at");
                                return new Snapshot(
                                        row.getLong("pending_count"),
                                        oldest == null ? null : oldest.toInstant());
                            });
        } catch (org.springframework.dao.DataAccessException unavailable) {
            // A failed sample is unknown, never a healthy zero or a stale successful sample.
            snapshot = null;
        }
    }

    private double unpublished() {
        var current = snapshot;
        return current == null ? Double.NaN : current.count();
    }

    private double oldestAge() {
        var current = snapshot;
        if (current == null) return Double.NaN;
        if (current.oldest() == null) return 0;
        return Math.max(0, Duration.between(current.oldest(), clock.instant()).toMillis() / 1000.0);
    }

    private record Snapshot(long count, Instant oldest) {}
}

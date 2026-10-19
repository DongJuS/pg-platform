package com.shinhan.pg.common.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** [CMN-F01] JdbcOutboxStore의 실제 PostgreSQL 위 동작 계약. Flyway 없이 같은 DDL을 쓴다. */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class JdbcOutboxStoreIT {
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static final String DDL =
            "create table outbox_events ("
                    + " id varchar(36) primary key,"
                    + " event_id varchar(36) not null,"
                    + " destination varchar(255) not null,"
                    + " routing_key varchar(255),"
                    + " payload bytea not null,"
                    + " claim_token varchar(36),"
                    + " claimed_at timestamptz null,"
                    + " published_at timestamptz null,"
                    + " attempts int not null default 0,"
                    + " created_at timestamptz not null default now())";

    JdbcOutboxStore store;
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(source);
        jdbc.execute("drop table if exists outbox_events");
        jdbc.execute(DDL);
        store = new JdbcOutboxStore(jdbc);
    }

    @Test
    void claimPublishesAndConsumesInOrder() {
        var first = record("first");
        var second = record("second");
        store.append(first);
        store.append(second);

        var token = UUID.randomUUID();
        var claimed = store.claim(10, token, Duration.ofMinutes(2));
        assertEquals(2, claimed.size());
        assertEquals("first", new String(claimed.get(0).payload(), StandardCharsets.UTF_8));
        assertEquals(first.eventId(), claimed.get(0).eventId());

        // 아직 published가 아니면 다음 claim은 임대가 유효한 동안 행을 돌려주지 않는다.
        assertTrue(store.claim(10, UUID.randomUUID(), Duration.ofMinutes(2)).isEmpty());

        store.published(first.id(), token);
        assertEquals(1L, countPublished());
        // 임대 토큰이 다른 published는 무시한다.
        store.published(second.id(), UUID.randomUUID());
        assertEquals(1L, countPublished());

        store.retry(second.id(), token);
        assertEquals(1, attemptsOf(second.id()));
        var reclaimed = store.claim(10, UUID.randomUUID(), Duration.ofMinutes(2));
        assertEquals(1, reclaimed.size());
        assertEquals(second.id(), reclaimed.get(0).id());
    }

    @Test
    void staleLeaseIsClaimedAgain() {
        var stale = record("stale");
        store.append(stale);
        // 즉시 만료되는 임대로 첫 claim을 만든 뒤, 같은 임대 기준으로 다른 워커가 다시 가져간다.
        store.claim(10, UUID.randomUUID(), Duration.ofMillis(-5));
        var reclaimed = store.claim(10, UUID.randomUUID(), Duration.ofMillis(-5));
        assertEquals(1, reclaimed.size());
        assertEquals(stale.id(), reclaimed.get(0).id());
    }

    private OutboxRecord record(String body) {
        return new OutboxRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "test.destination",
                "aggregate-key",
                body.getBytes(StandardCharsets.UTF_8));
    }

    private long countPublished() {
        return jdbc.queryForObject(
                "select count(*) from outbox_events where published_at is not null", Long.class);
    }

    private int attemptsOf(UUID id) {
        return jdbc.queryForObject(
                "select attempts from outbox_events where id = ?", Integer.class, id.toString());
    }
}

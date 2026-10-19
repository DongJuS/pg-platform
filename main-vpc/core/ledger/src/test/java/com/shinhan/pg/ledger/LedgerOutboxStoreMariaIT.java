package com.shinhan.pg.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.ledger.adapter.out.persistence.outbox.LedgerOutboxStore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * [CMN-F01] LedgerOutboxStore의 실제 MariaDB 11 위 동작 계약.
 * V1+V2 Flyway 마이그레이션으로 만든 outbox_events에 대해 append/claim/published/retry와
 * claim의 {@code FOR UPDATE SKIP LOCKED} 실동작을 검증한다.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class LedgerOutboxStoreMariaIT {
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> MARIADB =
            new GenericContainer<>(DockerImageName.parse("mariadb:11"))
                    .withEnv("MARIADB_ROOT_PASSWORD", "test")
                    .withEnv("MARIADB_DATABASE", "ledger_db")
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forListeningPort());

    static DriverManagerDataSource source;
    static JdbcTemplate jdbc;

    LedgerOutboxStore store;

    @BeforeAll
    static void migrate() {
        var url =
                "jdbc:mariadb://"
                        + MARIADB.getHost()
                        + ":"
                        + MARIADB.getMappedPort(3306)
                        + "/ledger_db";
        source = new DriverManagerDataSource(url, "root", "test");
        var result =
                Flyway.configure()
                        .dataSource(source)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
        assertEquals(3, result.migrationsExecuted);
        jdbc = new JdbcTemplate(source);
    }

    @BeforeEach
    void setUp() {
        jdbc.execute("delete from outbox_events");
        store = new LedgerOutboxStore(jdbc);
    }

    /** append → claim(임대 획득) → 다른 토큰 claim 미선택(임대 보호) → published(토큰 일치 시만) → retry(임대 해제·attempts 증가) → 재claim. */
    @Test
    void appendClaimPublishedRetryLifecycle() {
        var first = record("first");
        var second = record("second");
        store.append(first);
        store.append(second);

        // claim: 2건 모두 tokenA로 임대 획득
        var tokenA = UUID.randomUUID();
        var claimed = store.claim(10, tokenA, Duration.ofMinutes(2));
        assertEquals(2, claimed.size());
        assertEquals(
                Set.of(first.id(), second.id()),
                claimed.stream().map(OutboxRecord::id).collect(Collectors.toSet()));
        assertEquals(tokenA.toString(), claimTokenOf(first.id()));
        assertEquals(tokenA.toString(), claimTokenOf(second.id()));

        // 임대 보호: 다른 토큰의 claim은 임대 유효 기간 동안 0건
        assertTrue(store.claim(10, UUID.randomUUID(), Duration.ofMinutes(2)).isEmpty());

        // published는 임대 토큰이 일치할 때만 반영된다.
        store.published(first.id(), UUID.randomUUID());
        assertEquals(0L, countPublished());
        store.published(first.id(), tokenA);
        assertEquals(1L, countPublished());

        // retry: 임대 해제 + attempts 증가, 다음 claim에서 다시 선택된다.
        store.retry(second.id(), tokenA);
        assertEquals(1, attemptsOf(second.id()));
        assertNull(claimTokenOf(second.id()));
        assertNull(claimedAtOf(second.id()));

        var reclaimed = store.claim(10, UUID.randomUUID(), Duration.ofMinutes(2));
        assertEquals(1, reclaimed.size());
        assertEquals(second.id(), reclaimed.get(0).id());
        assertEquals(
                "second",
                new String(reclaimed.get(0).payload(), StandardCharsets.UTF_8));
    }

    /**
     * SKIP LOCKED 실동작: 다른 트랜잭션이 FOR UPDATE로 잠근 행은 claim이 건너뛰고(skip),
     * 잠기지 않은 행만 가져온다. SKIP LOCKED가 무시되면 claim이 innodb_lock_wait_timeout
     * (기본 50초)까지 블록되므로, 10초 안에 완료되는지도 함께 본다.
     */
    @Test
    void claimSkipsRowsLockedByAnotherTransaction() throws Exception {
        var locked = record("locked");
        var free = record("free");
        store.append(locked);
        store.append(free);

        var locker = source.getConnection();
        locker.setAutoCommit(false);
        try (var ps =
                locker.prepareStatement("select id from outbox_events where id = ? for update")) {
            ps.setString(1, locked.id().toString());
            ps.executeQuery();
        }

        var executor = Executors.newSingleThreadExecutor();
        try {
            var future =
                    executor.submit(
                            () -> store.claim(10, UUID.randomUUID(), Duration.ofMinutes(2)));
            var claimed = future.get(10, TimeUnit.SECONDS);
            assertEquals(1, claimed.size());
            assertEquals(free.id(), claimed.get(0).id());
        } finally {
            executor.shutdownNow();
            locker.rollback();
            locker.close();
        }
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

    private String claimTokenOf(UUID id) {
        return jdbc.queryForObject(
                "select claim_token from outbox_events where id = ?",
                String.class,
                id.toString());
    }

    private Object claimedAtOf(UUID id) {
        return jdbc.queryForObject(
                "select claimed_at from outbox_events where id = ?",
                Object.class,
                id.toString());
    }

    private int attemptsOf(UUID id) {
        return jdbc.queryForObject(
                "select attempts from outbox_events where id = ?", Integer.class, id.toString());
    }
}

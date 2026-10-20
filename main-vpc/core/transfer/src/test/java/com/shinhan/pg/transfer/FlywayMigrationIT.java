package com.shinhan.pg.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * [CMN-F01] 서비스 Flyway 마이그레이션이 실제 PostgreSQL에서 적용되고 outbox_events를 만드는지 검증한다. WO-A6: V3
 * transfers/transfer_histories와 멱등 unique(actor_user_id, idempotency_key) 제약을 함께 검증한다.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class FlywayMigrationIT {
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        var source =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var result =
                Flyway.configure()
                        .dataSource(source)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
        // V1 baseline + V2 outbox + V3 transfers(WO-A6)
        assertEquals(4, result.migrationsExecuted);
        jdbc = new JdbcTemplate(source);
    }

    @Test
    void migrationsCreateOutboxEvents() {
        var tables =
                jdbc.queryForList(
                        "select table_name from information_schema.tables"
                                + " where table_name = 'outbox_events'",
                        String.class);
        assertEquals(1, tables.size());
        jdbc.execute(
                "insert into outbox_events (id, event_id, destination, routing_key, payload)"
                        + " values ('00000000-0000-0000-0000-000000000001',"
                        + " '00000000-0000-0000-0000-000000000002', 'dest', 'key', 'x'::bytea)");
        assertTrue(jdbc.queryForObject("select count(*) from outbox_events", Long.class) == 1L);
    }

    @Test
    void v3CreatesTransferTablesWithIdempotencyConstraint() {
        var tables =
                jdbc.queryForList(
                        "select table_name from information_schema.tables"
                                + " where table_name in ('transfers', 'transfer_histories')",
                        String.class);
        assertEquals(2, tables.size());

        var uniques =
                jdbc.queryForList(
                        "select constraint_name from information_schema.table_constraints"
                                + " where table_name = 'transfers' and constraint_type = 'UNIQUE'",
                        String.class);
        assertEquals(2, uniques.size(), "transaction_id + (actor_user_id, idempotency_key) unique");

        // ADR 0001: 멱등키는 DB unique 제약이 강제한다(Redis 아님).
        jdbc.execute(
                "insert into transfers (id, actor_user_id, from_account_id, to_account_id, amount,"
                        + " currency, idempotency_key, request_hash, transaction_id, status) values"
                        + " ('00000000-0000-0000-0000-0000000000a1',"
                        + " '00000000-0000-0000-0000-0000000000b1',"
                        + " '00000000-0000-0000-0000-0000000000c1',"
                        + " '00000000-0000-0000-0000-0000000000c2', 1000, 'KRW', 'key-1', 'h1',"
                        + " '00000000-0000-0000-0000-0000000000d1', 'RECEIVED')");
        assertThrows(
                DuplicateKeyException.class,
                () ->
                        jdbc.execute(
                                "insert into transfers (id, actor_user_id, from_account_id,"
                                        + " to_account_id, amount, currency, idempotency_key,"
                                        + " request_hash, transaction_id, status) values"
                                        + " ('00000000-0000-0000-0000-0000000000a2',"
                                        + " '00000000-0000-0000-0000-0000000000b1',"
                                        + " '00000000-0000-0000-0000-0000000000c1',"
                                        + " '00000000-0000-0000-0000-0000000000c2', 2000, 'KRW',"
                                        + " 'key-1', 'h2', '00000000-0000-0000-0000-0000000000d2',"
                                        + " 'RECEIVED')"));
        // 같은 키라도 actor가 다르면 별개 요청이다.
        jdbc.execute(
                "insert into transfers (id, actor_user_id, from_account_id, to_account_id, amount,"
                        + " currency, idempotency_key, request_hash, transaction_id, status) values"
                        + " ('00000000-0000-0000-0000-0000000000a3',"
                        + " '00000000-0000-0000-0000-0000000000b2',"
                        + " '00000000-0000-0000-0000-0000000000c1',"
                        + " '00000000-0000-0000-0000-0000000000c2', 2000, 'KRW', 'key-1', 'h2',"
                        + " '00000000-0000-0000-0000-0000000000d3', 'RECEIVED')");
    }
}

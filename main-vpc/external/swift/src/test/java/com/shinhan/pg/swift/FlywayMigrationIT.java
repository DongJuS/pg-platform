package com.shinhan.pg.swift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** [CMN-F01] 서비스 Flyway 마이그레이션이 실제 PostgreSQL에서 적용되고 outbox_events를 만드는지 검증한다. */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class FlywayMigrationIT {
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migrationsCreateOutboxEvents() {
        var source =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var result =
                Flyway.configure()
                        .dataSource(source)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
        assertEquals(3, result.migrationsExecuted);

        var jdbc = new JdbcTemplate(source);
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
}

package com.shinhan.pg.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** [CMN-F01] 서비스 Flyway 마이그레이션이 실제 MariaDB에서 적용되고 outbox_events를 만드는지 검증한다. */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class FlywayMigrationIT {
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> MARIADB =
            new GenericContainer<>(DockerImageName.parse("mariadb:11"))
                    .withEnv("MARIADB_ROOT_PASSWORD", "test")
                    .withEnv("MARIADB_DATABASE", "ledger_db")
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forListeningPort());

    @Test
    void migrationsCreateOutboxEvents() {
        var url =
                "jdbc:mariadb://"
                        + MARIADB.getHost()
                        + ":"
                        + MARIADB.getMappedPort(3306)
                        + "/ledger_db";
        var source = new DriverManagerDataSource(url, "root", "test");
        var result =
                Flyway.configure()
                        .dataSource(source)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
        assertEquals(4, result.migrationsExecuted);

        var jdbc = new JdbcTemplate(source);
        // V3: 원장 업무 테이블(ledger_accounts/journals/postings)이 함께 생성된다.
        var businessTables =
                jdbc.queryForList(
                        "select table_name from information_schema.tables"
                                + " where table_schema = 'ledger_db'"
                                + " and table_name in ('ledger_accounts', 'journals', 'postings')",
                        String.class);
        assertEquals(3, businessTables.size());
        var tables =
                jdbc.queryForList(
                        "select table_name from information_schema.tables"
                                + " where table_schema = 'ledger_db'"
                                + " and table_name = 'outbox_events'",
                        String.class);
        assertEquals(1, tables.size());
        jdbc.execute(
                "insert into outbox_events (id, event_id, destination, routing_key, payload)"
                        + " values ('00000000-0000-0000-0000-000000000001',"
                        + " '00000000-0000-0000-0000-000000000002', 'dest', 'key', 'x')");
        assertTrue(jdbc.queryForObject("select count(*) from outbox_events", Long.class) == 1L);
    }
}

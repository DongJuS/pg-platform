package com.shinhan.pg.mockpg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.mockpg.api.PgOperationRequest;
import com.shinhan.pg.mockpg.scenario.PgOperationStore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class PgOperationStoreIT {
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void durableProviderResultSurvivesNewStoreAndRefundsCannotExceedApproval() {
        var source =
                new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        var store = new PgOperationStore(source);
        var payment = UUID.randomUUID();
        var id = UUID.randomUUID();
        var request =
                new PgOperationRequest(id, payment, "APPROVAL", new BigDecimal("30000"), "KRW");
        var first = store.submit(request, "APPROVED");
        var restarted = new PgOperationStore(source);
        assertEquals(first, restarted.find(id));
        assertEquals(first, restarted.submit(request, "APPROVED"));
        assertEquals(
                "APPROVED",
                store.submit(
                                new PgOperationRequest(
                                        UUID.randomUUID(),
                                        payment,
                                        "REFUND",
                                        new BigDecimal("20000"),
                                        "KRW"),
                                "APPROVED")
                        .outcome());
        assertEquals(
                "DECLINED",
                store.submit(
                                new PgOperationRequest(
                                        UUID.randomUUID(),
                                        payment,
                                        "REFUND",
                                        new BigDecimal("20000"),
                                        "KRW"),
                                "APPROVED")
                        .outcome());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        restarted.submit(
                                new PgOperationRequest(
                                        id, payment, "APPROVAL", BigDecimal.ONE, "KRW"),
                                "APPROVED"));
    }
}

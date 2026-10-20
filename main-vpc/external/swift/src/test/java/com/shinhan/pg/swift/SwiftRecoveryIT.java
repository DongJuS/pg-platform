package com.shinhan.pg.swift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.Mt103Subset;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.swift.adapter.out.persistence.SwiftSubmissionJdbcAdapter;
import com.shinhan.pg.swift.adapter.out.persistence.outbox.SwiftOutboxStore;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class SwiftRecoveryIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static DriverManagerDataSource ds;
    static JdbcTemplate jdbc;
    static final String KEY = java.util.Base64.getEncoder().encodeToString(new byte[32]);

    @BeforeAll
    static void migrate() {
        ds =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        jdbc = new JdbcTemplate(ds);
    }

    SwiftSubmissionJdbcAdapter store;
    Remittance r;
    String xml;

    @BeforeEach
    void initialize() {
        jdbc.execute("delete from swift_result_inbox");
        jdbc.execute("delete from swift_private_audit");
        jdbc.execute("delete from swift_messages");
        jdbc.execute("delete from outbox_events");
        store = new SwiftSubmissionJdbcAdapter(ds, new SwiftOutboxStore(jdbc), KEY);
        r =
                new Remittance(
                        UUID.randomUUID(),
                        UUID.randomUUID().toString(),
                        "REF123",
                        LocalDate.of(2026, 10, 20),
                        "SENDER",
                        "SOURCE123",
                        "PRIVATE RECEIVER",
                        "BENEFICIARY123",
                        "TESTUS33XXX",
                        new BigDecimal("10.00"),
                        "USD",
                        null,
                        "SHA");
        xml = Iso20022.payment(r, Instant.EPOCH);
        store.register(r, Mt103Subset.write(r), xml);
    }

    String report(String id, String code) {
        return Iso20022.status(
                new Iso20022.Result(id, r.reference(), r.uetr(), code), Instant.EPOCH);
    }

    void result(String id, String code) {
        String x = report(id, code);
        store.result(r.uetr(), x, Iso20022.result(x));
    }

    long count() {
        return jdbc.queryForObject("select count(*) from outbox_events", Long.class);
    }

    @Test
    void concurrentDeliveriesAndInitialSendHaveOneDurableClaim() throws Exception {
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 20; i++)
                jobs.add(pool.submit(() -> store.claim(r.uetr(), UUID.randomUUID(), false, 3, 1)));
            int claimed = 0;
            for (var j : jobs) if (j.get()) claimed++;
            assertEquals(1, claimed);
        }
        assertEquals(1, store.find(r.uetr()).orElseThrow().attempts());
    }

    @Test
    void duplicateAndOutOfOrderResultsEmitOneFinancialOutcome() {
        result("PENDING", "ACSP");
        assertEquals(0, count());
        result("FINAL", "ACSC");
        result("FINAL", "ACSC");
        result("OLD", "ACSP");
        assertEquals("ACSC", store.find(r.uetr()).orElseThrow().status());
        assertEquals(1, count());
    }

    @Test
    void conflictingFinalAndMutatedEventRequireReview() {
        result("FINAL", "ACSC");
        result("REJECT", "RJCT");
        result("FINAL", "ACSC");
        assertEquals("REVIEW", store.find(r.uetr()).orElseThrow().status());
        assertEquals(2, count());
    }

    @Test
    void sameReportIdentityWithDifferentBytesRequiresReview() {
        result("SAME", "ACSP");
        result("SAME", "ACSC");
        assertEquals("REVIEW", store.find(r.uetr()).orElseThrow().status());
        assertEquals(1, count());
    }

    @Test
    void unknownCodesNeverSettle() {
        result("UNKNOWN", "ZZZZ");
        assertEquals("REVIEW", store.find(r.uetr()).orElseThrow().status());
    }

    @Test
    void mismatchedOriginalReferenceRollsBack() {
        String x =
                Iso20022.status(
                        new Iso20022.Result("BAD", "WRONG", r.uetr(), "ACSC"), Instant.EPOCH);
        assertThrows(SwiftProblem.class, () -> store.result(r.uetr(), x, Iso20022.result(x)));
        assertEquals(0, count());
        assertEquals(
                0, jdbc.queryForObject("select count(*) from swift_result_inbox", Integer.class));
    }

    @Test
    void outboxFailureRollsBackStateAndInbox() {
        var brokenOutbox = org.mockito.Mockito.mock(com.shinhan.pg.common.outbox.OutboxStore.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("injected"))
                .when(brokenOutbox)
                .append(org.mockito.ArgumentMatchers.any());
        var broken = new SwiftSubmissionJdbcAdapter(ds, brokenOutbox, KEY);
        String x = report("FINAL", "ACSC");
        assertThrows(RuntimeException.class, () -> broken.result(r.uetr(), x, Iso20022.result(x)));
        assertEquals("READY", store.find(r.uetr()).orElseThrow().status());
        assertEquals(
                0, jdbc.queryForObject("select count(*) from swift_result_inbox", Integer.class));
    }

    @Test
    void abandonedSendIsUnknownAfterRestartAndIsNeverAnInitialReplay() {
        store.claim(r.uetr(), UUID.randomUUID(), false, 3, 1);
        jdbc.update(
                "update swift_messages set lease_until=now()-interval '1"
                        + " second',last_attempt_at=now()-interval '10 seconds'");
        store = new SwiftSubmissionJdbcAdapter(ds, new SwiftOutboxStore(jdbc), KEY);
        store.due();
        assertEquals("UNKNOWN", store.find(r.uetr()).orElseThrow().status());
        assertFalse(store.claim(r.uetr(), UUID.randomUUID(), false, 3, 1));
        assertTrue(store.claim(r.uetr(), UUID.randomUUID(), true, 3, 1));
        assertEquals(xml, store.find(r.uetr()).orElseThrow().xml());
    }

    @Test
    void minimumIntervalAndAttemptBudgetBlockResend() {
        var worker = UUID.randomUUID();
        assertTrue(store.claim(r.uetr(), worker, false, 1, 30));
        store.sent(r.uetr(), worker, true);
        assertFalse(store.claim(r.uetr(), UUID.randomUUID(), true, 3, 30));
        jdbc.update("update swift_messages set last_attempt_at=now()-interval '60 seconds'");
        assertFalse(store.claim(r.uetr(), UUID.randomUUID(), true, 1, 30));
    }

    @Test
    void rawHistoryIsEncryptedAndExpiresWhileIdentitySurvives() {
        assertFalse(
                jdbc.queryForObject("select pacs008 from swift_messages", String.class)
                        .contains("PRIVATE RECEIVER"));
        assertTrue(store.history(r.uetr(), true).toString().contains("PRIVATE RECEIVER"));
        assertFalse(store.history(r.uetr(), false).toString().contains("PRIVATE RECEIVER"));
        jdbc.update("update swift_messages set private_expires_at=now()-interval '1 second'");
        jdbc.update("update swift_private_audit set expires_at=now()-interval '1 second'");
        store.purge();
        assertNull(store.find(r.uetr()).orElseThrow().xml());
        assertTrue(store.history(r.uetr(), true).isEmpty());
        assertEquals(1, jdbc.queryForObject("select count(*) from swift_messages", Integer.class));
    }
}

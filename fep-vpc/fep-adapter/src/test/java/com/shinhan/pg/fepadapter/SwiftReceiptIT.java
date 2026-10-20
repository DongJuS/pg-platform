package com.shinhan.pg.fepadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.swiftmessages.Iso20022;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.fepadapter.adapter.out.persistence.SwiftReceiptJdbcAdapter;
import com.shinhan.pg.fepadapter.application.command.SendSwiftMessageCommand;

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
class SwiftReceiptIT {
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

    SwiftReceiptJdbcAdapter store;
    Remittance r;
    String xml;
    SendSwiftMessageCommand command;

    @BeforeEach
    void initialize() {
        jdbc.execute("delete from swift_result_reports");
        jdbc.execute("delete from swift_transmission_attempts");
        jdbc.execute("delete from swift_receipts");
        store = new SwiftReceiptJdbcAdapter(ds, KEY);
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
        command =
                new SendSwiftMessageCommand(
                        UUID.randomUUID(), "swift", r.transferId(), r.uetr(), r.reference(), xml);
        store.register(command, r.reference());
    }

    @Test
    void duplicateReceiptReusesMessageIdAndDoesNotResubmit() {
        var original = store.find(r.uetr()).orElseThrow();
        var worker = UUID.randomUUID();
        assertTrue(store.claim(r.uetr(), worker, false, 3, 1));
        store.sent(r.uetr(), worker, false);
        assertEquals(original.messageId(), store.register(command, r.reference()).messageId());
        assertFalse(store.claim(r.uetr(), UUID.randomUUID(), false, 3, 1));
        assertFalse(store.claim(r.uetr(), UUID.randomUUID(), true, 3, 1));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from swift_transmission_attempts", Integer.class));
    }

    @Test
    void sameUetrDifferentPayloadIsRejected() {
        var changed =
                new SendSwiftMessageCommand(
                        UUID.randomUUID(),
                        "swift",
                        r.transferId(),
                        r.uetr(),
                        r.reference(),
                        xml.replace("10.00", "11.00"));
        assertThrows(SwiftProblem.class, () -> store.register(changed, r.reference()));
        assertEquals(xml, store.find(r.uetr()).orElseThrow().xml());
    }

    @Test
    void unknownSendCanBeExplicitlyRetriedWithSameStoredBytesAfterInterval() {
        var worker = UUID.randomUUID();
        store.claim(r.uetr(), worker, false, 3, 1);
        store.sent(r.uetr(), worker, true);
        jdbc.update("update swift_receipts set last_attempt_at=now()-interval '10 seconds'");
        store = new SwiftReceiptJdbcAdapter(ds, KEY);
        assertTrue(store.claim(r.uetr(), UUID.randomUUID(), true, 3, 1));
        assertEquals(xml, store.find(r.uetr()).orElseThrow().xml());
        assertEquals(
                2,
                jdbc.queryForObject(
                        "select count(*) from swift_transmission_attempts", Integer.class));
    }

    @Test
    void reportHistoryPreservesConflictsAndDeduplicates() {
        String a =
                Iso20022.status(
                        new Iso20022.Result("A", r.reference(), r.uetr(), "ACSC"), Instant.EPOCH);
        String b =
                Iso20022.status(
                        new Iso20022.Result("B", r.reference(), r.uetr(), "RJCT"), Instant.EPOCH);
        store.result(r.uetr(), a);
        store.result(r.uetr(), a);
        store.result(r.uetr(), b);
        assertEquals(2, store.reports(r.uetr()).size());
    }

    @Test
    void wrongCorrelationAndInvalidXmlHaveNoSideEffects() {
        String bad =
                Iso20022.status(new Iso20022.Result("A", "WRONG", r.uetr(), "ACSC"), Instant.EPOCH);
        assertThrows(SwiftProblem.class, () -> store.result(r.uetr(), bad));
        assertThrows(
                SwiftProblem.class, () -> store.result(r.uetr(), "<!DOCTYPE invalid><Document/>"));
        assertTrue(store.reports(r.uetr()).isEmpty());
    }

    @Test
    void expiryRemovesPayloadButKeepsDedupeRecord() {
        jdbc.update("update swift_receipts set private_expires_at=now()-interval '1 second'");
        store.purge();
        assertNull(store.find(r.uetr()).orElseThrow().xml());
        assertFalse(store.claim(r.uetr(), UUID.randomUUID(), false, 3, 1));
    }

    @Test
    void abandonedFepSendingLeaseIsReportedAsUnknownAfterRestart() {
        store.claim(r.uetr(), UUID.randomUUID(), false, 3, 1);
        jdbc.update(
                "update swift_receipts set lease_until=now()-interval '1"
                        + " second',last_attempt_at=now()-interval '10 seconds'");
        store = new SwiftReceiptJdbcAdapter(ds, KEY);
        assertEquals("UNKNOWN", store.find(r.uetr()).orElseThrow().outcome());
        assertTrue(store.claim(r.uetr(), UUID.randomUUID(), true, 3, 1));
        assertEquals(xml, store.find(r.uetr()).orElseThrow().xml());
    }
}

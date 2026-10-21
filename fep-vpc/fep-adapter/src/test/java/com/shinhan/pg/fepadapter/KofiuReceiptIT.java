package com.shinhan.pg.fepadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.fepadapter.adapter.out.persistence.KofiuReceiptAdapter;
import com.shinhan.pg.fepadapter.application.port.out.KofiuGatewayPort;
import com.shinhan.pg.fepadapter.application.service.KofiuFepService;

import org.flywaydb.core.Flyway;
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
import java.util.List;
import java.util.UUID;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class KofiuReceiptIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    KofiuReceiptAdapter store;
    JdbcTemplate jdbc;
    DriverManagerDataSource ds;

    @BeforeEach
    void setup() {
        ds =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("truncate kofiu_attempts,kofiu_receipts cascade");
        store = new KofiuReceiptAdapter(ds, new ObjectMapper().findAndRegisterModules());
    }

    AmlReportDocument doc() {
        UUID id = UUID.randomUUID();
        return new AmlReportDocument(
                id,
                "report_" + id,
                UUID.randomUUID(),
                "STR_SIMULATION",
                UUID.randomUUID(),
                "KRW",
                new BigDecimal("100"),
                1,
                "fixture-v1",
                70,
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-21T00:00:00Z"),
                List.of("SYNTHETIC=70"));
    }

    KofiuResult result(AmlReportDocument d, String state) {
        return new KofiuResult(
                d.reportId(),
                d.reportKey(),
                d.hash(),
                "MOCK",
                "SENT",
                state,
                "NOT_RECEIVED".equals(state) ? 0 : 1,
                "ACCEPTED".equals(state) ? 1 : 0);
    }

    @Test
    void concurrentSubmissionsHaveOneInstitutionSend() throws Exception {
        var d = doc();
        var sends = new java.util.concurrent.atomic.AtomicInteger();
        KofiuGatewayPort port =
                new KofiuGatewayPort() {
                    public KofiuResult send(AmlReportDocument v) {
                        sends.incrementAndGet();
                        return result(v, "ACCEPTED");
                    }

                    public KofiuResult query(AmlReportDocument v) {
                        return result(v, "ACCEPTED");
                    }
                };
        var service = new KofiuFepService(store, port, false, 3, 30);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
            for (int i = 0; i < 16; i++)
                jobs.add(
                        () -> {
                            service.submit("aml", d);
                            return null;
                        });
            for (var job : pool.invokeAll(jobs)) job.get();
        }
        assertEquals(1, sends.get());
        assertEquals(1, jdbc.queryForObject("select count(*) from kofiu_attempts", Integer.class));
    }

    @Test
    void sameKeyDifferentContentOrIdentityIsConflict() {
        var d = doc();
        store.register(d);
        var changed =
                new AmlReportDocument(
                        d.reportId(),
                        d.reportKey(),
                        d.caseId(),
                        d.reportType(),
                        d.customerId(),
                        d.currency(),
                        new BigDecimal("101"),
                        d.transactionCount(),
                        d.ruleVersion(),
                        d.riskScore(),
                        d.windowStart(),
                        d.windowEnd(),
                        d.reasons());
        assertThrows(AmlProblem.class, () -> store.register(changed));
        var identity =
                new AmlReportDocument(
                        UUID.randomUUID(),
                        d.reportKey(),
                        d.caseId(),
                        d.reportType(),
                        d.customerId(),
                        d.currency(),
                        d.amount(),
                        d.transactionCount(),
                        d.ruleVersion(),
                        d.riskScore(),
                        d.windowStart(),
                        d.windowEnd(),
                        d.reasons());
        assertThrows(AmlProblem.class, () -> store.register(identity));
    }

    @Test
    void acceptedThenResponseLostIsUnknownUntilQueryAndNeverResent() {
        var d = doc();
        var sends = new java.util.concurrent.atomic.AtomicInteger();
        KofiuGatewayPort port =
                new KofiuGatewayPort() {
                    public KofiuResult send(AmlReportDocument v) {
                        sends.incrementAndGet();
                        throw new AmlProblem("LOST", ErrorType.EXTERNAL_FAILURE);
                    }

                    public KofiuResult query(AmlReportDocument v) {
                        return result(v, "ACCEPTED");
                    }
                };
        var service = new KofiuFepService(store, port, true, 3, 1);
        assertEquals("UNKNOWN", service.submit("aml", d).transmissionStatus());
        service.submit("aml", d);
        assertEquals(1, sends.get());
        assertEquals("ACCEPTED", service.query("aml", d.reportKey()).institutionStatus());
        assertThrows(AmlProblem.class, () -> service.retry("aml", d.reportKey()));
        assertEquals(1, sends.get());
    }

    @Test
    void restartPreservesReceiptAndStaleSendBecomesUnknown() {
        var d = doc();
        store.register(d);
        UUID worker = UUID.randomUUID();
        assertTrue(store.claim(d.reportKey(), worker, false, 3, 1));
        jdbc.update("update kofiu_receipts set lease_until=now()-interval '1 second'");
        var restarted = new KofiuReceiptAdapter(ds, new ObjectMapper().findAndRegisterModules());
        assertEquals("UNKNOWN", restarted.get(d.reportKey()).transmissionStatus());
        assertFalse(restarted.claim(d.reportKey(), UUID.randomUUID(), false, 3, 1));
    }

    @Test
    void actualResendRequiresFreshAuthoritativeAbsenceIntervalAndBudget() {
        var d = doc();
        store.register(d);
        UUID w = UUID.randomUUID();
        store.claim(d.reportKey(), w, false, 2, 1);
        store.finish(d.reportKey(), w, null);
        store.observe(d.reportKey(), result(d, "NOT_RECEIVED"));
        assertFalse(store.claim(d.reportKey(), UUID.randomUUID(), true, 2, 1));
        jdbc.update("update kofiu_receipts set last_sent_at=now()-interval '2 seconds'");
        UUID second = UUID.randomUUID();
        assertTrue(store.claim(d.reportKey(), second, true, 2, 1));
        store.finish(d.reportKey(), second, null);
        jdbc.update("update kofiu_receipts set last_sent_at=now()-interval '2 seconds'");
        assertFalse(store.claim(d.reportKey(), UUID.randomUUID(), true, 2, 1));
    }

    @Test
    void oldNotReceivedCannotAuthorizeSendWhenFreshQueryFails() {
        var d = doc();
        store.register(d);
        UUID w = UUID.randomUUID();
        store.claim(d.reportKey(), w, false, 3, 1);
        store.finish(d.reportKey(), w, null);
        store.observe(d.reportKey(), result(d, "NOT_RECEIVED"));
        jdbc.update("update kofiu_receipts set last_sent_at=now()-interval '2 seconds'");
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var service =
                new KofiuFepService(
                        store,
                        new KofiuGatewayPort() {
                            public KofiuResult send(AmlReportDocument v) {
                                calls.incrementAndGet();
                                return result(v, "ACCEPTED");
                            }

                            public KofiuResult query(AmlReportDocument v) {
                                throw new AmlProblem("DOWN", ErrorType.EXTERNAL_FAILURE);
                            }
                        },
                        true,
                        3,
                        1);
        assertThrows(AmlProblem.class, () -> service.retry("aml", d.reportKey()));
        assertEquals(0, calls.get());
    }

    @Test
    void conflictingFinalAndIntermediateResultsRemainSafe() {
        var d = doc();
        store.register(d);
        store.observe(d.reportKey(), result(d, "ACCEPTED"));
        store.observe(d.reportKey(), result(d, "PENDING"));
        assertEquals("ACCEPTED", store.get(d.reportKey()).institutionStatus());
        store.observe(d.reportKey(), result(d, "REJECTED"));
        assertEquals("REVIEW", store.get(d.reportKey()).institutionStatus());
    }

    @Test
    void mismatchedResultRollsBackAndStaleWorkerCannotFinish() {
        var d = doc();
        store.register(d);
        UUID w = UUID.randomUUID();
        store.claim(d.reportKey(), w, false, 3, 1);
        store.finish(d.reportKey(), UUID.randomUUID(), result(d, "ACCEPTED"));
        assertEquals("SENDING", store.get(d.reportKey()).transmissionStatus());
        assertThrows(
                AmlProblem.class, () -> store.finish(d.reportKey(), w, result(doc(), "ACCEPTED")));
        assertEquals("UNKNOWN", store.get(d.reportKey()).institutionStatus());
    }

    @Test
    void otherAuthenticatedServiceCannotUseAmlInstitutionPort() {
        var port =
                new KofiuGatewayPort() {
                    public KofiuResult send(AmlReportDocument d) {
                        throw new AssertionError();
                    }

                    public KofiuResult query(AmlReportDocument d) {
                        throw new AssertionError();
                    }
                };
        var service = new KofiuFepService(store, port, false, 3, 30);
        assertThrows(AmlProblem.class, () -> service.submit("payment", doc()));
        assertEquals(0, jdbc.queryForObject("select count(*) from kofiu_receipts", Integer.class));
    }

    @Test
    void expiredReceiptCannotBeResubmittedAfterPrivateContentDeletion() {
        var d = doc();
        store.register(d);
        jdbc.update("update kofiu_receipts set created_at=now()-interval '49 hours'");
        assertThrows(AmlProblem.class, () -> store.get(d.reportKey()));
        store.purgeExpired();
        assertEquals(0, jdbc.queryForObject("select count(*) from kofiu_receipts", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject("select count(*) from kofiu_expired_reports", Integer.class));
        assertThrows(AmlProblem.class, () -> store.register(d));
    }
}

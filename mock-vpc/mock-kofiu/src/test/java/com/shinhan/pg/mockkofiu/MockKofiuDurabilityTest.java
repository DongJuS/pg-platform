package com.shinhan.pg.mockkofiu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.mockkofiu.api.MockKofiuController;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

class MockKofiuDurabilityTest {
    @TempDir Path directory;

    MockKofiuController mock() {
        var e =
                new MockEnvironment()
                        .withProperty("pg.aml.mock-token", "synthetic-fep")
                        .withProperty("pg.aml.mock-operations-token", "synthetic-ops")
                        .withProperty("pg.aml.mock-directory", directory.toString());
        e.setActiveProfiles("aml-test");
        return new MockKofiuController(new ObjectMapper().findAndRegisterModules(), e);
    }

    AmlReportDocument doc() {
        UUID id = UUID.randomUUID();
        return new AmlReportDocument(
                id,
                "report_" + id,
                UUID.randomUUID(),
                "STR_SIMULATION",
                UUID.randomUUID(),
                "USD",
                new BigDecimal("100"),
                1,
                "fixture-v1",
                70,
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-21T00:00:00Z"),
                List.of("TEST=70"));
    }

    @Test
    void duplicateDeliveryAfterRestartHasOneAcceptance() {
        var d = doc();
        var first = mock();
        assertEquals(1, first.send("synthetic-fep", d).acceptances());
        var restarted = mock();
        var result = restarted.send("synthetic-fep", d);
        assertEquals(2, result.deliveries());
        assertEquals(1, result.acceptances());
    }

    @Test
    void sameKeyChangedContentConflictsAfterRestart() {
        var d = doc();
        mock().send("synthetic-fep", d);
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
        assertThrows(AmlProblem.class, () -> mock().send("synthetic-fep", changed));
    }

    @Test
    void pendingAndFinalAcceptanceAreDistinct() {
        var d = doc();
        var m = mock();
        m.scenario("synthetic-ops", new MockKofiuController.Scenario(d.reportKey(), "PENDING"));
        assertEquals("PENDING", m.send("synthetic-fep", d).institutionStatus());
        assertEquals(
                "ACCEPTED",
                m.finalizeResult(
                                "synthetic-ops",
                                new MockKofiuController.FinalResult(d.reportKey(), "ACCEPTED"))
                        .institutionStatus());
        assertThrows(
                AmlProblem.class,
                () ->
                        m.finalizeResult(
                                "synthetic-ops",
                                new MockKofiuController.FinalResult(d.reportKey(), "REJECTED")));
    }

    @Test
    void credentialsAndPathTraversalAreRejected() {
        var m = mock();
        var d = doc();
        assertThrows(AmlProblem.class, () -> m.send("forged", d));
        assertThrows(
                AmlProblem.class,
                () ->
                        m.scenario(
                                "synthetic-fep",
                                new MockKofiuController.Scenario(d.reportKey(), "SUCCESS")));
        assertThrows(
                AmlProblem.class,
                () ->
                        m.query(
                                "synthetic-fep",
                                new MockKofiuController.Query(
                                        d.reportId(), "../../escape", d.hash())));
    }
}

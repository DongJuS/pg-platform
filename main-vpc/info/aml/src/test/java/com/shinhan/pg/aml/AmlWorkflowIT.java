package com.shinhan.pg.aml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.aml.adapter.out.persistence.AmlWorkflowAdapter;
import com.shinhan.pg.aml.adapter.out.persistence.outbox.AmlOutboxStore;
import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.aml.domain.model.DetectionRule;
import com.shinhan.pg.aml.domain.model.FinancialFact;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.outbox.OutboxRecord;
import com.shinhan.pg.common.outbox.OutboxStore;

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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Real PostgreSQL, synthetic policy only. No H2 and no regulatory threshold. */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class AmlWorkflowIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    DriverManagerDataSource ds;
    JdbcTemplate jdbc;
    AmlWorkflowAdapter store;
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final UUID customer = UUID.randomUUID(), source = UUID.randomUUID(), target = UUID.randomUUID();
    final Instant at = Instant.parse("2026-10-21T00:00:00Z");

    AmlRuleSet policy(String version, BigDecimal min, int count) {
        return new AmlRuleSet(
                version,
                70,
                List.of(
                        new DetectionRule(
                                "SYNTHETIC_AMOUNT",
                                version,
                                "Synthetic fixture",
                                70,
                                "KRW",
                                Set.of("DOMESTIC_TRANSFER"),
                                min,
                                count,
                                86400)));
    }

    FinancialFact fact(UUID id, BigDecimal amount, Instant when) {
        return new FinancialFact(
                id, id, "DOMESTIC_TRANSFER", customer, source, target, amount, "KRW", when, null);
    }

    EvaluateTransactionCommand command(UUID id, String topic) {
        return new EvaluateTransactionCommand(
                UUID.randomUUID(), UUID.randomUUID(), id, topic, UUID.randomUUID());
    }

    @BeforeEach
    void setup() {
        ds =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        jdbc = new JdbcTemplate(ds);
        jdbc.execute(
                "truncate"
                    + " aml_audit,aml_reports,aml_cases,aml_rules,aml_rule_sets,aml_facts,aml_inbox,outbox_events"
                    + " cascade");
        store = new AmlWorkflowAdapter(ds, json, new AmlOutboxStore(jdbc));
    }

    void accept(EvaluateTransactionCommand c, FinancialFact f, AmlRuleSet p) {
        store.receive(c);
        store.resolve(c, f, null, p);
    }

    long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    @Test
    void sameFactAcrossTopicsAndConcurrentDeliveriesProducesOneReport() throws Exception {
        var id = UUID.randomUUID();
        var f = fact(id, new BigDecimal("100"), at);
        var p = policy("fixture-v1", new BigDecimal("100"), 1);
        var jobs = new java.util.ArrayList<java.util.concurrent.Callable<Void>>();
        for (int i = 0; i < 12; i++) {
            var c = command(id, i % 2 == 0 ? "ledger.posted" : "transfer.completed");
            store.receive(c);
            jobs.add(
                    () -> {
                        store.resolve(c, f, null, p);
                        return null;
                    });
        }
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
            for (var future : pool.invokeAll(jobs)) future.get();
        }
        assertEquals(12, count("aml_inbox"));
        assertEquals(1, count("aml_facts"));
        assertEquals(1, count("aml_cases"));
        assertNotNull(store.list(null, null, null, 0, 20).getFirst().createdAt());
        assertEquals(1, count("aml_reports"));
        assertEquals(1, count("outbox_events"));
    }

    @Test
    void belowBoundaryDoesNotGenerateReport() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("99.9999"), at),
                policy("v1", new BigDecimal("100"), 1));
        assertEquals(0, count("aml_reports"));
        assertEquals("NO_REPORT", store.list(null, null, null, 0, 20).getFirst().status());
    }

    @Test
    void exactBoundaryFreezesVersionAndReasons() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("100"), at),
                policy("v1", new BigDecimal("100"), 1));
        var d = store.readyReports(10).getFirst();
        assertEquals("v1", d.ruleVersion());
        assertEquals(List.of("SYNTHETIC_AMOUNT=70"), d.reasons());
        assertEquals(1, d.transactionCount());
        assertEquals("STR_SIMULATION", d.reportType());
    }

    @Test
    void noPolicyPreservesSourceWithoutDeclaringNormal() {
        var id = UUID.randomUUID();
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), null);
        assertEquals(1, count("aml_facts"));
        assertEquals(0, count("aml_cases"));
        assertEquals(
                "POLICY_PENDING",
                jdbc.queryForObject("select reason from aml_inbox", String.class));
        store.reevaluate(policy("approved-later-fixture", new BigDecimal("100"), 1));
        assertEquals(1, count("aml_reports"));
    }

    @Test
    void lateEvidenceReevaluatesFutureAnchors() {
        var later = UUID.randomUUID();
        var earlier = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 2);
        accept(command(later, "ledger.posted"), fact(later, new BigDecimal("60"), at), p);
        assertEquals(0, count("aml_reports"));
        accept(
                command(earlier, "ledger.posted"),
                fact(earlier, new BigDecimal("40"), at.minusSeconds(86399)),
                p);
        assertEquals(1, count("aml_reports"));
        assertEquals(2, store.readyReports(10).getFirst().transactionCount());
    }

    @Test
    void outsideWindowIsNotIncluded() {
        var x = UUID.randomUUID();
        var y = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 2);
        accept(
                command(x, "ledger.posted"),
                fact(x, new BigDecimal("50"), at.minusSeconds(86401)),
                p);
        accept(command(y, "ledger.posted"), fact(y, new BigDecimal("50"), at), p);
        assertEquals(0, count("aml_reports"));
    }

    @Test
    void crossCurrencyCannotRaiseKrwScore() {
        var x = UUID.randomUUID();
        var y = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 2);
        accept(
                command(x, "ledger.posted"),
                new FinancialFact(
                        x,
                        x,
                        "OVERSEAS_TRANSFER",
                        customer,
                        source,
                        null,
                        new BigDecimal("100000"),
                        "USD",
                        at,
                        null),
                p);
        accept(command(y, "ledger.posted"), fact(y, new BigDecimal("50"), at), p);
        assertEquals(0, count("aml_reports"));
    }

    @Test
    void reversalIsLinkedAndNeverNewTurnover() {
        var x = UUID.randomUUID();
        accept(
                command(x, "ledger.posted"),
                new FinancialFact(
                        x,
                        x,
                        "PAYMENT_REFUND",
                        customer,
                        source,
                        target,
                        new BigDecimal("1000"),
                        "KRW",
                        at,
                        UUID.randomUUID()),
                policy("v1", new BigDecimal("100"), 1));
        assertEquals(1, count("aml_facts"));
        assertEquals(0, count("aml_cases"));
        assertEquals(0, count("aml_reports"));
    }

    @Test
    void holdIsExplicitlyExcluded() {
        var c = command(UUID.randomUUID(), "ledger.posted");
        store.receive(c);
        store.resolve(c, null, "OVERSEAS_HOLD", policy("v1", new BigDecimal("100"), 1));
        assertEquals(0, count("aml_facts"));
        assertEquals("EXCLUDED", jdbc.queryForObject("select status from aml_inbox", String.class));
    }

    @Test
    void eventIdentityMutationIsConflict() {
        var c = command(UUID.randomUUID(), "ledger.posted");
        store.receive(c);
        assertThrows(
                AmlProblem.class,
                () ->
                        store.receive(
                                new EvaluateTransactionCommand(
                                        UUID.randomUUID(),
                                        c.eventId(),
                                        UUID.randomUUID(),
                                        c.topic(),
                                        c.journalId())));
        assertEquals(1, count("aml_inbox"));
    }

    @Test
    void changedSourceEvidenceIsReviewAndOriginalRemainsFrozen() {
        var id = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), p);
        var d = store.readyReports(10).getFirst();
        var c = command(id, "transfer.completed");
        accept(c, fact(id, new BigDecimal("200"), at), p);
        assertEquals(
                "REVIEW",
                jdbc.queryForObject(
                        "select status from aml_inbox where event_id=?",
                        String.class,
                        c.eventId().toString()));
        assertEquals(d.hash(), store.reportForCase(d.caseId()).hash());
    }

    @Test
    void reportEvidenceCannotBeOverwrittenByLateEvents() {
        var first = UUID.randomUUID();
        var late = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(first, "ledger.posted"), fact(first, new BigDecimal("100"), at), p);
        var frozen = store.readyReports(10).getFirst();
        accept(
                command(late, "ledger.posted"),
                fact(late, new BigDecimal("50"), at.minusSeconds(1)),
                p);
        assertEquals("REVIEW", store.caseById(frozen.caseId()).status());
        assertEquals(frozen.hash(), store.reportForCase(frozen.caseId()).hash());
    }

    @Test
    void changedRuleUnderSameVersionRollsBack() {
        var x = UUID.randomUUID();
        accept(
                command(x, "ledger.posted"),
                fact(x, new BigDecimal("100"), at),
                policy("v1", new BigDecimal("100"), 1));
        var y = UUID.randomUUID();
        var c = command(y, "ledger.posted");
        store.receive(c);
        assertThrows(
                AmlProblem.class,
                () ->
                        store.resolve(
                                c,
                                fact(y, new BigDecimal("100"), at),
                                null,
                                policy("v1", new BigDecimal("200"), 1)));
        assertEquals(1, count("aml_facts"));
        assertEquals(
                "PENDING",
                jdbc.queryForObject(
                        "select status from aml_inbox where event_id=?",
                        String.class,
                        c.eventId().toString()));
    }

    @Test
    void reevaluationIsIdempotentAndNewVersionIsSeparate() {
        var id = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), p);
        store.reevaluate(p);
        store.reevaluate(p);
        assertEquals(1, count("aml_reports"));
        store.reevaluate(policy("v2", new BigDecimal("100"), 1));
        assertEquals(2, count("aml_reports"));
        assertEquals(1, count("aml_facts"));
    }

    @Test
    void outboxFailureRollsBackEntireEvaluation() {
        var actual = new AmlOutboxStore(jdbc);
        OutboxStore failing =
                new OutboxStore() {
                    public void append(OutboxRecord r) {
                        actual.append(r);
                        throw new IllegalStateException("injected");
                    }

                    public List<OutboxRecord> claim(int l, UUID w, Duration d) {
                        return List.of();
                    }

                    public void published(UUID i, UUID t) {}

                    public void retry(UUID i, UUID t) {}
                };
        var broken = new AmlWorkflowAdapter(ds, json, failing);
        var id = UUID.randomUUID();
        var c = command(id, "ledger.posted");
        store.receive(c);
        assertThrows(
                IllegalStateException.class,
                () ->
                        broken.resolve(
                                c,
                                fact(id, new BigDecimal("100"), at),
                                null,
                                policy("v1", new BigDecimal("100"), 1)));
        assertEquals(0, count("aml_facts"));
        assertEquals(0, count("aml_reports"));
        assertEquals(0, count("outbox_events"));
        assertEquals("PENDING", jdbc.queryForObject("select status from aml_inbox", String.class));
    }

    @Test
    void inboxSurvivesAdapterRestartAndUnavailableSource() {
        var c = command(UUID.randomUUID(), "ledger.posted");
        store.receive(c);
        store.defer(c, "SOURCE_UNAVAILABLE");
        assertEquals(0, count("aml_facts"));
        jdbc.update("update aml_inbox set next_attempt_at=now()-interval '1 second'");
        var restarted = new AmlWorkflowAdapter(ds, json, new AmlOutboxStore(jdbc));
        assertEquals(c.transactionId(), restarted.pending(10).getFirst().transactionId());
    }

    @Test
    void onlyOneWorkerCanSendAndStaleWorkerCannotComplete() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("100"), at),
                policy("v1", new BigDecimal("100"), 1));
        var d = store.readyReports(10).getFirst();
        UUID worker = UUID.randomUUID();
        assertTrue(store.claimReport(d.reportId(), worker));
        assertFalse(store.claimReport(d.reportId(), UUID.randomUUID()));
        var result =
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "ACCEPTED", 1, 1);
        store.reportResult(d, UUID.randomUUID(), result);
        assertEquals("UNKNOWN", store.reportState(d.reportId()).institutionStatus());
        store.reportResult(d, worker, result);
        assertEquals("ACCEPTED", store.reportState(d.reportId()).institutionStatus());
    }

    @Test
    void timeoutDoesNotCancelDecisionAndQueryRecoversSameId() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("100"), at),
                policy("v1", new BigDecimal("100"), 1));
        var d = store.readyReports(10).getFirst();
        UUID worker = UUID.randomUUID();
        store.claimReport(d.reportId(), worker);
        store.reportResult(d, worker, null);
        assertEquals("UNKNOWN", store.reportState(d.reportId()).transmissionStatus());
        assertEquals("REPORT_REQUIRED", store.caseById(d.caseId()).status());
        store.reportResult(
                d,
                null,
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "ACCEPTED", 1, 1));
        assertEquals("ACCEPTED", store.reportState(d.reportId()).institutionStatus());
        assertEquals(1, count("aml_reports"));
    }

    @Test
    void contradictoryFinalIsReviewAndIntermediateDoesNotDowngrade() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("100"), at),
                policy("v1", new BigDecimal("100"), 1));
        var d = store.readyReports(10).getFirst();
        store.reportResult(
                d,
                null,
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "ACCEPTED", 1, 1));
        store.reportResult(
                d,
                null,
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "PENDING", 1, 0));
        assertEquals("ACCEPTED", store.reportState(d.reportId()).institutionStatus());
        store.reportResult(
                d,
                null,
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "REJECTED", 1, 0));
        assertEquals("REVIEW", store.reportState(d.reportId()).institutionStatus());
    }

    @Test
    void amendmentFreezesNewContentAndReusesIdentityOnRedelivery() {
        var id = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), p);
        var d = store.readyReports(10).getFirst();
        store.reportResult(
                d,
                null,
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "ACCEPTED", 1, 1));
        var late = UUID.randomUUID();
        accept(
                command(late, "ledger.posted"),
                fact(late, new BigDecimal("50"), at.minusSeconds(1)),
                p);
        var amendment = store.amend(d.caseId(), d.reportId());
        assertEquals(2, amendment.revision());
        assertEquals(d.reportId(), amendment.supersedesReportId());
        assertNotEquals(d.hash(), amendment.hash());
        assertEquals(new BigDecimal("1.5E+2"), amendment.amount());
        assertEquals(amendment.reportId(), store.amend(d.caseId(), d.reportId()).reportId());
        assertEquals(2, count("aml_reports"));
        assertEquals(2, count("outbox_events"));
    }

    @Test
    void amendmentWithoutNewEvidenceCannotCreateAnotherRevision() {
        var id = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), p);
        var d = store.readyReports(10).getFirst();
        store.reportResult(
                d,
                null,
                new KofiuResult(
                        d.reportId(), d.reportKey(), d.hash(), "FEP", "SENT", "ACCEPTED", 1, 1));
        var late = UUID.randomUUID();
        accept(
                command(late, "ledger.posted"),
                fact(late, new BigDecimal("50"), at.minusSeconds(1)),
                p);
        var amendment = store.amend(d.caseId(), d.reportId());
        store.reportResult(
                amendment,
                null,
                new KofiuResult(
                        amendment.reportId(),
                        amendment.reportKey(),
                        amendment.hash(),
                        "FEP2",
                        "SENT",
                        "ACCEPTED",
                        1,
                        1));
        assertThrows(AmlProblem.class, () -> store.amend(d.caseId(), amendment.reportId()));
        assertEquals(2, count("aml_reports"));
    }

    @Test
    void privateDataExpiresAndHashOnlyTombstonePreventsFreshReplay() {
        var id = UUID.randomUUID();
        var c = command(id, "ledger.posted");
        var f = fact(id, new BigDecimal("100"), at);
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(c, f, p);
        jdbc.update("update aml_facts set occurred_at=now()-interval '49 hours'");
        jdbc.update("update aml_cases set created_at=now()-interval '49 hours'");
        jdbc.update("update aml_reports set created_at=now()-interval '49 hours'");
        jdbc.update("update aml_inbox set created_at=now()-interval '49 hours'");
        jdbc.update("update aml_audit set created_at=now()-interval '49 hours'");
        store.purgeExpired();
        assertEquals(0, count("aml_facts"));
        assertEquals(0, count("aml_cases"));
        assertEquals(0, count("aml_reports"));
        assertEquals(0, count("aml_audit"));
        assertEquals(1, count("aml_expired_effects"));
        var replay = command(id, "ledger.posted");
        accept(replay, f, p);
        assertEquals(0, count("aml_reports"));
        assertEquals(
                "SOURCE_EXPIRED",
                jdbc.queryForObject("select reason from aml_inbox", String.class));
    }

    @Test
    void oldSourceFactsRemainReviewInsteadOfBeingDeclaredNormal() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("100"), Instant.now().minusSeconds(172801)),
                policy("v1", new BigDecimal("100"), 1));
        assertEquals(0, count("aml_facts"));
        assertEquals(0, count("aml_cases"));
        assertEquals(
                "SOURCE_EXPIRED",
                jdbc.queryForObject("select reason from aml_inbox", String.class));
    }

    @Test
    void automaticQueryBudgetConvertsUncertaintyToReviewWithoutLosingDecision() {
        var id = UUID.randomUUID();
        accept(
                command(id, "ledger.posted"),
                fact(id, new BigDecimal("100"), at),
                policy("v1", new BigDecimal("100"), 1));
        var d = store.readyReports(10).getFirst();
        store.reportResult(d, null, null);
        jdbc.update(
                "update aml_reports set query_attempts=12,next_query_at=now()-interval '1 second'");
        assertTrue(store.unresolvedReports(10).isEmpty());
        assertEquals("REVIEW", store.reportState(d.reportId()).workflowStatus());
        assertEquals("REPORT_REQUIRED", store.caseById(d.caseId()).status());
    }

    @Test
    void sameBusinessCannotBecomeAnotherFinancialEffectUnderDifferentTransaction() {
        var id = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), p);
        var other = UUID.randomUUID();
        var c = command(other, "ledger.posted");
        accept(
                c,
                new FinancialFact(
                        other,
                        id,
                        "DOMESTIC_TRANSFER",
                        customer,
                        source,
                        target,
                        new BigDecimal("100"),
                        "KRW",
                        at,
                        null),
                p);
        assertEquals(1, count("aml_facts"));
        assertEquals(1, count("aml_reports"));
        assertEquals(
                "BUSINESS_EFFECT_CONFLICT",
                jdbc.queryForObject(
                        "select reason from aml_inbox where event_id=?",
                        String.class,
                        c.eventId().toString()));
    }

    @Test
    void lateEvidenceDoesNotIntroduceHumanApprovalBeforeAlreadyConfirmedInitialReport() {
        var id = UUID.randomUUID();
        var p = policy("v1", new BigDecimal("100"), 1);
        accept(command(id, "ledger.posted"), fact(id, new BigDecimal("100"), at), p);
        var d = store.readyReports(10).getFirst();
        var late = UUID.randomUUID();
        accept(
                command(late, "ledger.posted"),
                fact(late, new BigDecimal("50"), at.minusSeconds(1)),
                p);
        assertEquals("REVIEW", store.caseById(d.caseId()).status());
        assertTrue(store.claimReport(d.reportId(), UUID.randomUUID()));
        assertEquals(d.hash(), store.reportForCase(d.caseId()).hash());
    }
}

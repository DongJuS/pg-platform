package com.shinhan.pg.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.outbox.JdbcOutboxStore;
import com.shinhan.pg.payment.adapter.out.persistence.PaymentPersistenceAdapter;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.readmodel.PgResult;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.PaymentOperation;

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
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class PaymentStoreIT {
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    DriverManagerDataSource source;
    JdbcTemplate jdbc;
    PaymentPersistenceAdapter store;
    UUID actor;

    @BeforeEach
    void init() {
        source = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        jdbc.execute(
                "truncate"
                    + " reconciliation_differences,reconciliation_runs,pg_events,payment_operations,payments,outbox_events"
                    + " cascade");
        store = new PaymentPersistenceAdapter(source, new JdbcOutboxStore(jdbc));
        actor = UUID.randomUUID();
    }

    ApprovePaymentCommand command(String key) {
        return new ApprovePaymentCommand(
                UUID.randomUUID(),
                actor,
                "order-" + key,
                UUID.randomUUID(),
                new BigDecimal("30000"),
                "KRW",
                key,
                UUID.randomUUID(),
                UUID.randomUUID());
    }

    PgResult result(PaymentOperation op, String outcome, long version, UUID event) {
        return new PgResult(
                op.id(),
                op.paymentId(),
                op.kind(),
                outcome,
                op.amount().amount(),
                "KRW",
                version,
                event,
                "MOCK-" + op.id(),
                Instant.parse("2026-10-20T00:00:00Z"));
    }

    PaymentOperation approved() {
        var op = store.reserveApproval(command("approve"));
        assertTrue(store.claimPg(op.id()));
        store.acceptResult(result(op, "APPROVED", 2, UUID.randomUUID()));
        assertTrue(store.claimLedger(op.id()));
        store.complete(op.id(), UUID.randomUUID());
        return op;
    }

    @Test
    void approvalRequiresPgAndJournalAndOutboxIsAtomic() {
        var op = store.reserveApproval(command("approval"));
        assertThrows(PaymentException.class, () -> store.complete(op.id(), UUID.randomUUID()));
        assertEquals("PROCESSING", store.payment(op.paymentId()).status());
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events", Integer.class));
        store.claimPg(op.id());
        store.acceptResult(result(op, "APPROVED", 1, UUID.randomUUID()));
        store.claimLedger(op.id());
        var broken =
                new PaymentPersistenceAdapter(
                        source,
                        new JdbcOutboxStore(jdbc) {
                            @Override
                            public void append(com.shinhan.pg.common.outbox.OutboxRecord r) {
                                throw new IllegalStateException("injected outbox failure");
                            }
                        });
        var journal = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> broken.complete(op.id(), journal));
        assertFalse(store.operation(op.id()).completed());
        assertEquals("PROCESSING", store.payment(op.paymentId()).status());
        store.complete(op.id(), journal);
        store.complete(op.id(), journal);
        assertEquals("APPROVED", store.payment(op.paymentId()).status());
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events", Integer.class));
    }

    @Test
    void concurrentRefundsIncludeReservedAmounts() throws Exception {
        var approval = approved();
        try (var pool = Executors.newFixedThreadPool(8)) {
            var calls = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 12; i++) {
                final String key = "refund-" + i;
                calls.add(
                        () -> {
                            try {
                                store.reserveAdjustment(
                                        actor,
                                        approval.id(),
                                        "REFUND",
                                        new BigDecimal("20000"),
                                        key);
                                return true;
                            } catch (PaymentException e) {
                                assertEquals(PaymentErrorCode.REFUND_LIMIT_EXCEEDED, e.code());
                                return false;
                            }
                        });
            }
            int accepted = 0;
            for (var future : pool.invokeAll(calls)) if (future.get()) accepted++;
            assertEquals(1, accepted);
            assertEquals(
                    0,
                    store.payment(approval.id())
                            .reserved()
                            .amount()
                            .compareTo(new BigDecimal("20000")));
        }
    }

    @Test
    void cancellationConflictsAndUnknownRefundKeepsCapacity() {
        var approval = approved();
        var refund =
                store.reserveAdjustment(
                        actor, approval.id(), "REFUND", new BigDecimal("10000"), "r");
        assertTrue(store.claimPg(refund.id()));
        store.unknown(refund.id(), "UNKNOWN");
        assertFalse(store.claimPg(refund.id()));
        assertThrows(
                PaymentException.class,
                () -> store.reserveAdjustment(actor, approval.id(), "CANCEL", null, "full-cancel"));
        assertEquals(
                0,
                store.payment(approval.id())
                        .reserved()
                        .amount()
                        .compareTo(new BigDecimal("10000")));
        store.acceptResult(result(refund, "APPROVED", 1, UUID.randomUUID()));
        store.claimLedger(refund.id());
        store.complete(refund.id(), UUID.randomUUID());
        assertEquals("PARTIALLY_REFUNDED", store.payment(approval.id()).status());
        assertThrows(
                PaymentException.class,
                () -> store.reserveAdjustment(actor, approval.id(), "CANCEL", null, "full-cancel"));
    }

    @Test
    void duplicateEventsAndOutOfOrderDeliveryHaveOneFinancialEffect() {
        var approval = approved();
        var op = store.operation(approval.id());
        store.acceptResult(result(op, "PENDING", 1, UUID.randomUUID()));
        store.acceptResult(result(op, "APPROVED", 2, UUID.randomUUID()));
        assertFalse(store.claimLedger(op.id()));
        assertFalse(store.claimPg(op.id()));
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events", Integer.class));
        assertEquals("APPROVED", store.payment(op.paymentId()).status());
    }

    @Test
    void eventIdReuseWithDifferentContentsIsRejected() {
        var op = store.reserveApproval(command("event"));
        var event = UUID.randomUUID();
        store.acceptResult(result(op, "PENDING", 1, event));
        assertThrows(
                PaymentException.class, () -> store.acceptResult(result(op, "APPROVED", 2, event)));
        assertEquals("PENDING", store.operation(op.id()).pgOutcome());
    }

    @Test
    void sameIdempotencyKeyReturnsOriginalWithoutResending() {
        var c = command("idem");
        var a = store.reserveApproval(c);
        var b = store.reserveApproval(c);
        assertEquals(a.id(), b.id());
        assertTrue(store.claimPg(a.id()));
        assertFalse(store.claimPg(b.id()));
        assertThrows(PaymentException.class, () -> store.reserveApproval(command("idem")));
    }

    @Test
    void databaseRejectsOverReservationEvenIfApplicationIsBypassed() {
        var op = approved();
        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () ->
                        jdbc.update(
                                "update payments set reserved_amount=40000 where id=?",
                                op.id().toString()));
        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () ->
                        jdbc.update(
                                "update payment_operations set status='COMPLETED',journal_id=null"
                                        + " where id=?",
                                op.id().toString()));
    }

    @Test
    void foreignOwnerCannotReserveRefund() {
        var op = approved();
        var e =
                assertThrows(
                        PaymentException.class,
                        () ->
                                store.reserveAdjustment(
                                        UUID.randomUUID(),
                                        op.id(),
                                        "REFUND",
                                        BigDecimal.ONE,
                                        "foreign"));
        assertEquals(PaymentErrorCode.FORBIDDEN, e.code());
    }

    @Test
    void reconciliationComparesActualLedgerAmountsAndPersistsEvidence() {
        var approval = approved();
        var op = store.operation(approval.id());
        var payment = store.payment(op.paymentId());
        var fep =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.FepPaymentPort.class);
        var ledger =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.LedgerPostingPort.class);
        var date = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        org.mockito.Mockito.when(fep.results(date))
                .thenReturn(java.util.List.of(result(op, "APPROVED", 2, UUID.randomUUID())));
        org.mockito.Mockito.when(ledger.audit(op.id()))
                .thenReturn(
                        new com.shinhan.pg.payment.application.readmodel.LedgerAudit(
                                op.id(),
                                java.util.List.of(
                                        new com.shinhan.pg.payment.application.readmodel.LedgerAudit
                                                .Line(
                                                payment.sourceAccountId(),
                                                new BigDecimal("-30000"),
                                                "KRW"),
                                        new com.shinhan.pg.payment.application.readmodel.LedgerAudit
                                                .Line(
                                                payment.destinationAccountId(),
                                                new BigDecimal("30000"),
                                                "KRW"))));
        var runs =
                new com.shinhan.pg.payment.adapter.out.persistence
                        .ReconciliationRunPersistenceAdapter(source, new JdbcOutboxStore(jdbc));
        var count = new java.util.concurrent.atomic.AtomicInteger();
        var service =
                new com.shinhan.pg.payment.application.service.PaymentReconciliationService(
                        store, runs, fep, ledger, count::addAndGet);
        var clean =
                service.startReconciliation(
                        new com.shinhan.pg.payment.application.command.StartReconciliationCommand(
                                UUID.randomUUID(), "operator", date));
        assertTrue(runs.get(clean.runId()).differences().isEmpty());
        org.mockito.Mockito.when(ledger.audit(op.id()))
                .thenReturn(
                        new com.shinhan.pg.payment.application.readmodel.LedgerAudit(
                                op.id(),
                                java.util.List.of(
                                        new com.shinhan.pg.payment.application.readmodel.LedgerAudit
                                                .Line(
                                                payment.sourceAccountId(),
                                                new BigDecimal("-29999"),
                                                "KRW"),
                                        new com.shinhan.pg.payment.application.readmodel.LedgerAudit
                                                .Line(
                                                payment.destinationAccountId(),
                                                new BigDecimal("29999"),
                                                "KRW"))));
        var mismatch =
                service.startReconciliation(
                        new com.shinhan.pg.payment.application.command.StartReconciliationCommand(
                                UUID.randomUUID(), "operator", date));
        assertEquals(
                "LEDGER_VALUE",
                runs.get(mismatch.runId()).differences().getFirst().differenceType());
        assertEquals(1, count.get());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from outbox_events where destination='recon.mismatch'",
                        Integer.class));
    }

    @Test
    void unavailableProviderNeverBecomesAZeroMismatchSuccess() {
        approved();
        var fep =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.FepPaymentPort.class);
        var ledger =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.LedgerPostingPort.class);
        var date = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        org.mockito.Mockito.when(fep.results(date))
                .thenThrow(new PaymentException(PaymentErrorCode.PG_RESULT_UNKNOWN));
        var runs =
                new com.shinhan.pg.payment.adapter.out.persistence
                        .ReconciliationRunPersistenceAdapter(source, new JdbcOutboxStore(jdbc));
        var service =
                new com.shinhan.pg.payment.application.service.PaymentReconciliationService(
                        store, runs, fep, ledger);
        assertThrows(
                PaymentException.class,
                () ->
                        service.startReconciliation(
                                new com.shinhan.pg.payment.application.command
                                        .StartReconciliationCommand(
                                        UUID.randomUUID(), "operator", date)));
        assertEquals(
                "FAILED",
                jdbc.queryForObject(
                        "select status from reconciliation_runs order by created_at desc limit 1",
                        String.class));
    }

    @Test
    void explicitlyEnabledQueryRecoveryNeverResubmitsFinancialRequest() {
        var c = command("query-recovery");
        var op = store.reserveApproval(c);
        store.claimPg(op.id());
        store.unknown(op.id(), "UNKNOWN");
        var fep =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.FepPaymentPort.class);
        var ledger =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.LedgerPostingPort.class);
        org.mockito.Mockito.when(fep.query(op.id()))
                .thenReturn(java.util.Optional.of(result(op, "APPROVED", 1, UUID.randomUUID())));
        org.mockito.Mockito.when(
                        ledger.post(
                                org.mockito.ArgumentMatchers.any(),
                                org.mockito.ArgumentMatchers.any()))
                .thenReturn(UUID.randomUUID());
        var app =
                new com.shinhan.pg.payment.application.service.PaymentApplicationService(
                        store, fep, ledger, (a, b, d, e) -> {}, (a, b) -> null, true);
        app.recoverPayment(
                new com.shinhan.pg.payment.application.command.RecoverPaymentCommand(
                        UUID.randomUUID(), op.id()));
        assertTrue(store.operation(op.id()).completed());
        org.mockito.Mockito.verify(fep, org.mockito.Mockito.never())
                .submit(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(ledger, org.mockito.Mockito.times(1))
                .post(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void lostLedgerResponseIsRecoveredByLookupWithoutASecondPosting() {
        var op = store.reserveApproval(command("lost-ledger"));
        store.claimPg(op.id());
        store.acceptResult(result(op, "APPROVED", 1, UUID.randomUUID()));
        store.claimLedger(op.id());
        store.unknown(op.id(), "LEDGER_UNKNOWN");
        var fep =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.FepPaymentPort.class);
        var ledger =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.LedgerPostingPort.class);
        org.mockito.Mockito.when(ledger.find(op.id()))
                .thenReturn(java.util.Optional.of(UUID.randomUUID()));
        var app =
                new com.shinhan.pg.payment.application.service.PaymentApplicationService(
                        store, fep, ledger, (a, b, d, e) -> {}, (a, b) -> null, true);
        app.recoverPayment(
                new com.shinhan.pg.payment.application.command.RecoverPaymentCommand(
                        UUID.randomUUID(), op.id()));
        assertTrue(store.operation(op.id()).completed());
        org.mockito.Mockito.verifyNoInteractions(fep);
        org.mockito.Mockito.verify(ledger, org.mockito.Mockito.never())
                .post(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void missingProviderLookupKeepsUnknownAndCannotAuthorizePosting() {
        var op = store.reserveApproval(command("missing-provider"));
        store.claimPg(op.id());
        store.unknown(op.id(), "UNKNOWN");
        var fep =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.FepPaymentPort.class);
        var ledger =
                org.mockito.Mockito.mock(
                        com.shinhan.pg.payment.application.port.out.LedgerPostingPort.class);
        org.mockito.Mockito.when(fep.query(op.id())).thenReturn(java.util.Optional.empty());
        var app =
                new com.shinhan.pg.payment.application.service.PaymentApplicationService(
                        store, fep, ledger, (a, b, d, e) -> {}, (a, b) -> null, true);
        assertThrows(
                PaymentException.class,
                () ->
                        app.recoverPayment(
                                new com.shinhan.pg.payment.application.command
                                        .RecoverPaymentCommand(UUID.randomUUID(), op.id())));
        assertEquals("UNKNOWN", store.operation(op.id()).status());
        org.mockito.Mockito.verifyNoInteractions(ledger);
    }
}

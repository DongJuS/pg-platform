package com.shinhan.pg.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.eventcontracts.OverseasLedgerAppliedEvent;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.adapter.out.persistence.OverseasTransferDetailsPersistenceAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.outbox.TransferOutboxStore;
import com.shinhan.pg.transfer.application.command.GetOverseasTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestOverseasTransferCommand;
import com.shinhan.pg.transfer.application.port.out.SwiftOperationsPort;
import com.shinhan.pg.transfer.application.readmodel.AccountAccessSnapshot;
import com.shinhan.pg.transfer.application.service.OverseasOperationsService;
import com.shinhan.pg.transfer.application.service.OverseasTransferService;

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
import java.util.List;
import java.util.UUID;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class OverseasTransferIT {
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

    OverseasTransferDetailsPersistenceAdapter store;
    UUID actor;
    UUID source;

    @BeforeEach
    void initialize() {
        jdbc.execute("delete from overseas_audit");
        jdbc.execute("delete from overseas_transfers");
        jdbc.execute("delete from outbox_events");
        store =
                new OverseasTransferDetailsPersistenceAdapter(
                        ds, new TransferOutboxStore(jdbc), KEY);
        actor = UUID.randomUUID();
        source = UUID.randomUUID();
    }

    RequestOverseasTransferCommand command(String key) {
        return new RequestOverseasTransferCommand(
                UUID.randomUUID(),
                actor,
                source,
                "PRIVATE RECEIVER",
                "BENEFICIARY123",
                "TESTUS33XXX",
                new BigDecimal("10.00"),
                "USD",
                key,
                "SYNTHETIC_PROOF");
    }

    OverseasTransferService service(
            com.shinhan.pg.transfer.application.port.out.OverseasLedgerReservationPort ledger) {
        return new OverseasTransferService(
                true,
                store,
                ledger,
                id -> new AccountAccessSnapshot(source, actor, "USD", "ACTIVE"),
                c -> {});
    }

    @Test
    void concurrentIdenticalRequestsReserveAndPublishOnlyOnce() throws Exception {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var service = service(r -> calls.incrementAndGet());
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<UUID>>();
            for (int i = 0; i < 20; i++)
                jobs.add(pool.submit(() -> service.request(command("concurrent")).transferId()));
            var ids = new java.util.HashSet<UUID>();
            for (var job : jobs) ids.add(job.get());
            assertEquals(1, ids.size());
        }
        assertEquals(1, calls.get());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from outbox_events where destination='transfer.outbound'",
                        Integer.class));
    }

    @Test
    void responseLossAndRestartConvergeUsingSameTransferAndUetr() {
        var service =
                service(
                        r -> {
                            throw new IllegalStateException("lost");
                        });
        var first = service.request(command("lost"));
        assertEquals("UNKNOWN", first.status());
        var stored = store.find(first.transferId()).orElseThrow();
        assertEquals(first.uetr(), stored.uetr());
        store =
                new OverseasTransferDetailsPersistenceAdapter(
                        ds, new TransferOutboxStore(jdbc), KEY);
        service(r -> {}).recoverPending();
        assertEquals("PROCESSING", store.find(first.transferId()).orElseThrow().status());
        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events", Integer.class));
    }

    @Test
    void sameKeyChangedRecipientDoesNotReserveAgain() {
        var svc = service(r -> {});
        svc.request(command("key"));
        var c = command("key");
        var changed =
                new RequestOverseasTransferCommand(
                        c.requestId(),
                        actor,
                        source,
                        "DIFFERENT RECEIVER",
                        c.recipientAccount(),
                        c.bic(),
                        c.amount(),
                        c.currency(),
                        "key",
                        c.otpToken());
        assertThrows(SwiftProblem.class, () -> svc.request(changed));
        assertEquals(
                1, jdbc.queryForObject("select count(*) from overseas_transfers", Integer.class));
    }

    @Test
    void sourceOwnershipAndQueryOwnershipAreEnforced() {
        var svc = service(r -> {});
        var c = command("key");
        var foreign =
                new RequestOverseasTransferCommand(
                        c.requestId(),
                        UUID.randomUUID(),
                        source,
                        c.recipientName(),
                        c.recipientAccount(),
                        c.bic(),
                        c.amount(),
                        c.currency(),
                        "foreign",
                        c.otpToken());
        assertThrows(SwiftProblem.class, () -> svc.request(foreign));
        var row = svc.request(c);
        assertThrows(
                SwiftProblem.class,
                () ->
                        svc.get(
                                new GetOverseasTransferCommand(
                                        UUID.randomUUID(), UUID.randomUUID(), row.transferId())));
        assertThrows(
                SwiftProblem.class,
                () ->
                        svc.get(
                                new GetOverseasTransferCommand(
                                        UUID.randomUUID(), null, row.transferId())));
    }

    @Test
    void ledgerAppliedDuplicatesEmitOneTransferCompleted() {
        var row = service(r -> {}).request(command("key"));
        var e =
                new OverseasLedgerAppliedEvent(
                        UUID.randomUUID(),
                        row.transferId(),
                        row.uetr(),
                        "ACSC",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now());
        store.applied(e);
        store.applied(e);
        assertEquals("COMPLETED", store.find(row.transferId()).orElseThrow().status());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from outbox_events where destination='transfer.completed'",
                        Integer.class));
    }

    @Test
    void rejectionAndConflictNeverCreateSuccess() {
        var row = service(r -> {}).request(command("key"));
        store.applied(
                new OverseasLedgerAppliedEvent(
                        UUID.randomUUID(),
                        row.transferId(),
                        row.uetr(),
                        "RJCT",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now()));
        assertEquals("REJECTED", store.find(row.transferId()).orElseThrow().status());
        store.applied(
                new OverseasLedgerAppliedEvent(
                        UUID.randomUUID(),
                        row.transferId(),
                        row.uetr(),
                        "ACSC",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now()));
        assertEquals("REVIEW", store.find(row.transferId()).orElseThrow().status());
        assertEquals(
                0,
                jdbc.queryForObject(
                        "select count(*) from outbox_events where destination='transfer.completed'",
                        Integer.class));
    }

    @Test
    void maskedOwnerAndRawAdministratorHaveSeparateViewsAndFortyEightHourExpiry() {
        var row = service(r -> {}).request(command("key"));
        var ops = org.mockito.Mockito.mock(SwiftOperationsPort.class);
        org.mockito.Mockito.when(ops.history(row.uetr())).thenReturn(List.of("RAW TEST"));
        UUID admin = UUID.randomUUID();
        var access = new OverseasOperationsService(java.util.Set.of(admin), store, ops);
        assertFalse(
                access.history(actor, row.transferId()).toString().contains("PRIVATE RECEIVER"));
        assertTrue(access.history(admin, row.transferId()).toString().contains("PRIVATE RECEIVER"));
        assertThrows(SwiftProblem.class, () -> access.history(UUID.randomUUID(), row.transferId()));
        assertThrows(SwiftProblem.class, () -> access.action(actor, row.transferId(), true));
        assertFalse(
                jdbc.queryForObject("select private_payload from overseas_transfers", String.class)
                        .contains("PRIVATE RECEIVER"));
        jdbc.update("update overseas_transfers set private_expires_at=now()-interval '1 second'");
        jdbc.update("update overseas_audit set expires_at=now()-interval '1 second'");
        store.purgePrivateData();
        assertTrue(access.history(admin, row.transferId()).isEmpty());
        assertEquals("PROCESSING", store.find(row.transferId()).orElseThrow().status());
    }

    @Test
    void realOverseasHttpDtoAcceptsExplicitUsdAmountAndOtp() throws Exception {
        var svc = service(r -> {});
        var controller =
                new com.shinhan.pg.transfer.adapter.in.web.OverseasTransferController(
                        svc::request, svc::get);
        var mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                                controller)
                        .setControllerAdvice(
                                new com.shinhan.pg.transfer.adapter.in.web.SwiftProblemHandler(),
                                new com.shinhan.pg.transfer.adapter.in.web
                                        .PlatformExceptionHandler())
                        .build();
        String json =
                "{\"fromAccountId\":\""
                        + source
                        + "\",\"recipientName\":\"PRIVATE SWIFT"
                        + " RECEIVER\",\"recipientAccount\":\"MOCK_SUCCESS\",\"bic\":\"TESTUS33XXX\",\"amount\":\"10.00\",\"currency\":\"USD\",\"otpToken\":\"SYNTHETIC_PROOF\"}";
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/transfers/overseas")
                                .header("X-User-Id", actor.toString())
                                .header("Idempotency-Key", "swift_test")
                                .contentType("application/json")
                                .content(json))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isAccepted());
    }
}

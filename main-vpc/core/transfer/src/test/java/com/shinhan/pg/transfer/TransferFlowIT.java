package com.shinhan.pg.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.TransferCompletedEvent;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferAtomicWriteAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.TransferPersistenceJdbcAdapter;
import com.shinhan.pg.transfer.adapter.out.persistence.outbox.TransferOutboxStore;
import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

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
import java.util.UUID;

/**
 * [WO-A6] transfer 금융 쓰기의 실 DB 검증: 예약 → 중복 409 → 완료 멱등 → transfer.completed Outbox 행.
 * PostgreSQL 컨테이너에서 실제 제약·조건부 update·같은 트랜잭션 Outbox를 확인한다.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class TransferFlowIT {
    private static final UUID ACTOR = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID FROM = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID TO = UUID.fromString("10000000-0000-0000-0000-000000000003");

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static TransferAtomicWriteAdapter atomicWrite;
    static TransferPersistenceJdbcAdapter queries;

    @BeforeAll
    static void migrate() {
        var source =
                new DriverManagerDataSource(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(source);
        atomicWrite = new TransferAtomicWriteAdapter(source, new TransferOutboxStore(jdbc));
        queries = new TransferPersistenceJdbcAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("delete from outbox_events");
        jdbc.execute("delete from transfer_histories");
        jdbc.execute("delete from transfers");
    }

    private RequestTransferCommand newCommand(String idempotencyKey) {
        return new RequestTransferCommand(
                UUID.randomUUID(),
                ACTOR,
                FROM,
                TO,
                new BigDecimal("1000"),
                "KRW",
                "mock-otp-token", // 필드 수용만 — 검증 미구현(WO-A6 TODO)
                idempotencyKey);
    }

    @Test
    void reserveCreatesProcessingTransferWithHistory() {
        var draft = atomicWrite.reserveRequest(newCommand("flow-reserve"));

        assertNotNull(draft.transferId());
        assertNotNull(draft.transactionId());
        assertEquals("PROCESSING", queries.findStatusById(draft.transferId()).orElseThrow());
        var histories = queries.findHistories(draft.transferId());
        assertEquals(2, histories.size());
        assertNull(histories.get(0).previousStatus());
        assertEquals("RECEIVED", histories.get(0).nextStatus());
        assertEquals("PROCESSING", histories.get(1).nextStatus());
    }

    @Test
    void duplicateIdempotencyKeyIsRejectedWith409Semantics() {
        var first = atomicWrite.reserveRequest(newCommand("flow-dup"));

        // 같은 키 + 같은 본문 — 재요청은 409이며 기존 transferId를 detail로 노출한다.
        var same =
                assertThrows(
                        TransferException.class,
                        () -> atomicWrite.reserveRequest(newCommand("flow-dup")));
        assertEquals(TransferErrorCode.DUPLICATE_REQUEST, same.code());
        assertEquals(first.transferId(), same.existingTransferId());

        // 같은 키 + 다른 본문 — 키 충돌도 409.
        var conflict =
                assertThrows(
                        TransferException.class,
                        () ->
                                atomicWrite.reserveRequest(
                                        new RequestTransferCommand(
                                                UUID.randomUUID(),
                                                ACTOR,
                                                FROM,
                                                TO,
                                                new BigDecimal("2000"),
                                                "KRW",
                                                null,
                                                "flow-dup")));
        assertEquals(TransferErrorCode.DUPLICATE_REQUEST, conflict.code());
        assertEquals(first.transferId(), conflict.existingTransferId());

        // 신규 이체는 만들어지지 않는다.
        assertEquals(1L, jdbc.queryForObject("select count(*) from transfers", Long.class));
    }

    @Test
    void completeOnceWritesOutboxAndAbsorbsDuplicates() {
        var draft = atomicWrite.reserveRequest(newCommand("flow-complete"));
        var journalId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var command =
                new CompleteTransferCommand(
                        eventId, draft.transferId(), draft.transactionId(), journalId);

        atomicWrite.completeOnceWithOutbox(command);

        assertEquals("COMPLETED", queries.findStatusById(draft.transferId()).orElseThrow());
        var outboxRows =
                jdbc.queryForList(
                        "select event_id, destination, routing_key, payload from outbox_events");
        assertEquals(1, outboxRows.size());
        var row = outboxRows.get(0);
        assertEquals(EventTopics.TRANSFER_COMPLETED, row.get("DESTINATION"));
        assertEquals(eventId.toString(), row.get("EVENT_ID"));
        assertEquals(draft.transferId().toString(), row.get("ROUTING_KEY"));
        var event =
                EventCodec.fromBytes((byte[]) row.get("PAYLOAD"), TransferCompletedEvent.class);
        assertEquals(eventId, event.eventId());
        assertEquals(draft.transferId(), event.transferId());
        assertEquals(draft.transactionId(), event.transactionId());
        assertEquals(journalId, event.journalId());

        // REST 완료와 ledger.posted 소비의 중복 도달(다른 eventId 포함) — 완료 처리는 한 번만.
        atomicWrite.completeOnceWithOutbox(command);
        atomicWrite.completeOnceWithOutbox(
                new CompleteTransferCommand(
                        UUID.randomUUID(), draft.transferId(), draft.transactionId(), journalId));
        assertEquals(1L, jdbc.queryForObject("select count(*) from outbox_events", Long.class));
        assertEquals(
                3L,
                jdbc.queryForObject(
                        "select count(*) from transfer_histories where transfer_id = ?",
                        Long.class,
                        draft.transferId().toString()));
    }

    @Test
    void unknownThenRecoveredToCompleted() {
        var draft = atomicWrite.reserveRequest(newCommand("flow-unknown"));

        atomicWrite.markUnknown(draft.transferId());
        assertEquals("UNKNOWN", queries.findStatusById(draft.transferId()).orElseThrow());

        // UNKNOWN 스캔 대상이 된다(F20 복구 경로).
        var unknowns = queries.findUnknown(10);
        assertEquals(1, unknowns.size());
        assertEquals(draft.transferId(), unknowns.get(0).transferId());
        assertEquals(draft.transactionId(), unknowns.get(0).transactionId());

        // 복구: 동일 transactionId의 journal 확인 → 완료로 수렴(UNKNOWN→COMPLETED 허용).
        atomicWrite.completeOnceWithOutbox(
                new CompleteTransferCommand(
                        UUID.randomUUID(),
                        draft.transferId(),
                        draft.transactionId(),
                        UUID.randomUUID()));
        assertEquals("COMPLETED", queries.findStatusById(draft.transferId()).orElseThrow());
        assertTrue(queries.findUnknown(10).isEmpty());
    }

    @Test
    void terminalStateIsNeverReversed() {
        var draft = atomicWrite.reserveRequest(newCommand("flow-terminal"));
        atomicWrite.completeOnceWithOutbox(
                new CompleteTransferCommand(
                        UUID.randomUUID(),
                        draft.transferId(),
                        draft.transactionId(),
                        UUID.randomUUID()));
        var historyCount =
                jdbc.queryForObject(
                        "select count(*) from transfer_histories where transfer_id = ?",
                        Long.class,
                        draft.transferId().toString());

        // 최종 상태 덮어쓰기 금지 — markUnknown/markFailed 모두 무변경.
        atomicWrite.markUnknown(draft.transferId());
        atomicWrite.markFailed(draft.transferId(), "LATE_REJECTION");
        assertEquals("COMPLETED", queries.findStatusById(draft.transferId()).orElseThrow());
        assertEquals(
                historyCount,
                jdbc.queryForObject(
                        "select count(*) from transfer_histories where transfer_id = ?",
                        Long.class,
                        draft.transferId().toString()));

        // FAILED → COMPLETED 역전은 거절된다.
        var failed = atomicWrite.reserveRequest(newCommand("flow-terminal-2"));
        atomicWrite.markFailed(failed.transferId(), "LEDGER_REJECTED");
        assertEquals("FAILED", queries.findStatusById(failed.transferId()).orElseThrow());
        var reversal =
                assertThrows(
                        TransferException.class,
                        () ->
                                atomicWrite.completeOnceWithOutbox(
                                        new CompleteTransferCommand(
                                                UUID.randomUUID(),
                                                failed.transferId(),
                                                failed.transactionId(),
                                                UUID.randomUUID())));
        assertEquals(TransferErrorCode.INVALID_STATE, reversal.code());
    }

    @Test
    void completeRejectsMismatchedTransactionAndUnknownTransfer() {
        var draft = atomicWrite.reserveRequest(newCommand("flow-guard"));

        var mismatch =
                assertThrows(
                        TransferException.class,
                        () ->
                                atomicWrite.completeOnceWithOutbox(
                                        new CompleteTransferCommand(
                                                UUID.randomUUID(),
                                                draft.transferId(),
                                                UUID.randomUUID(),
                                                UUID.randomUUID())));
        assertEquals(TransferErrorCode.INVALID_STATE, mismatch.code());

        var missing =
                assertThrows(
                        TransferException.class,
                        () ->
                                atomicWrite.completeOnceWithOutbox(
                                        new CompleteTransferCommand(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                UUID.randomUUID())));
        assertEquals(TransferErrorCode.NOT_FOUND, missing.code());
        assertEquals("PROCESSING", queries.findStatusById(draft.transferId()).orElseThrow());
    }

    @Test
    void queryPortFiltersAndPaginatesPerOwner() {
        var first = atomicWrite.reserveRequest(newCommand("flow-list-1"));
        var second = atomicWrite.reserveRequest(newCommand("flow-list-2"));
        var otherActorId = UUID.randomUUID();
        atomicWrite.reserveRequest(
                new RequestTransferCommand(
                        UUID.randomUUID(),
                        otherActorId,
                        FROM,
                        TO,
                        new BigDecimal("5000"),
                        "KRW",
                        null,
                        "flow-list-other"));

        // 본인 것만 조회된다(F17).
        assertEquals(1, queries.findSummaries(ACTOR, null, null, null, 0, 1).size());
        assertEquals(1, queries.findSummaries(ACTOR, null, null, null, 1, 10).size());
        assertEquals(2, queries.findSummaries(ACTOR, null, null, "PROCESSING", 0, 10).size());
        assertTrue(queries.findSummaries(ACTOR, null, null, "COMPLETED", 0, 10).isEmpty());
        assertEquals(1, queries.findSummaries(otherActorId, null, null, null, 0, 10).size());

        // transactionId로 이체 식별자를 해소한다(consumer 경로).
        assertEquals(
                first.transferId(),
                queries.findTransferIdByTransactionId(first.transactionId()).orElseThrow());
        assertEquals(
                second.transferId(),
                queries.findTransferIdByTransactionId(second.transactionId()).orElseThrow());
        assertTrue(queries.findTransferIdByTransactionId(UUID.randomUUID()).isEmpty());
    }
}

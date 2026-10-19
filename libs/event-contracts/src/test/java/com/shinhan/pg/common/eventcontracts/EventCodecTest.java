package com.shinhan.pg.common.eventcontracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.common.eventcontracts.EventCodec.EventSerializationException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventCodecTest {

    @Test
    void ledgerPostedRoundTrip() {
        LedgerPostedEvent event = new LedgerPostedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-18T01:30:00Z"));

        byte[] payload = EventCodec.toBytes(event);
        LedgerPostedEvent decoded = EventCodec.fromBytes(payload, LedgerPostedEvent.class);

        assertEquals(event, decoded);
    }

    @Test
    void transferCompletedRoundTrip() {
        TransferCompletedEvent event = new TransferCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-10-18T01:30:00Z"));

        byte[] payload = EventCodec.toBytes(event);
        TransferCompletedEvent decoded = EventCodec.fromBytes(payload, TransferCompletedEvent.class);

        assertEquals(event, decoded);
    }

    @Test
    void ledgerPostedPayloadContainsOnlyIdentifiers() {
        LedgerPostedEvent event = new LedgerPostedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-10-18T01:30:00Z"));

        String json = new String(EventCodec.toBytes(event), StandardCharsets.UTF_8);

        assertEquals(
                "{\"eventId\":\"" + event.eventId() + "\","
                        + "\"transactionId\":\"" + event.transactionId() + "\","
                        + "\"journalId\":\"" + event.journalId() + "\","
                        + "\"occurredAt\":\"2026-10-18T01:30:00Z\"}",
                json);
    }

    @Test
    void missingFieldFails() {
        // transactionId 누락 — record 생성자 검증으로 실패해야 한다.
        String json = "{\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"journalId\":\"" + UUID.randomUUID() + "\","
                + "\"occurredAt\":\"2026-10-18T01:30:00Z\"}";

        assertThrows(
                EventSerializationException.class,
                () -> EventCodec.fromBytes(json.getBytes(StandardCharsets.UTF_8), LedgerPostedEvent.class));
    }

    @Test
    void malformedJsonFails() {
        assertThrows(
                EventSerializationException.class,
                () -> EventCodec.fromBytes("not-json".getBytes(StandardCharsets.UTF_8), TransferCompletedEvent.class));
    }

    @Test
    void constructorRejectsNullFields() {
        assertThrows(
                NullPointerException.class,
                () -> new TransferCompletedEvent(null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        Instant.now()));
        assertThrows(
                NullPointerException.class,
                () -> new LedgerPostedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null));
    }
}

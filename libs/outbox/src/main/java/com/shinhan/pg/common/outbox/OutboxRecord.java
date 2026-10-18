package com.shinhan.pg.common.outbox;
import java.util.Objects;
import java.util.UUID;
public record OutboxRecord(UUID id, UUID eventId, String destination, String key, byte[] payload) {
    public OutboxRecord {
        Objects.requireNonNull(id); Objects.requireNonNull(eventId); Objects.requireNonNull(destination);
        payload=Objects.requireNonNull(payload).clone();
    }
    @Override public byte[] payload() { return payload.clone(); }
}

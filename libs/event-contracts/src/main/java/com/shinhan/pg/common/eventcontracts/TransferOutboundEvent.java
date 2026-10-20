package com.shinhan.pg.common.eventcontracts;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transfer publishes only after Ledger reservation is confirmed. Private fields use authenticated
 * REST.
 */
public record TransferOutboundEvent(
        UUID eventId, UUID transferId, String uetr, Instant occurredAt) {
    public TransferOutboundEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(uetr);
        Objects.requireNonNull(occurredAt);
    }
}

package com.shinhan.pg.common.eventcontracts;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Swift's fep.ack V1. Outcome is external, not evidence of Ledger completion. REVIEW never moves
 * money.
 */
public record SwiftAcknowledgedEvent(
        UUID eventId, UUID transferId, String uetr, String outcome, Instant occurredAt) {
    public SwiftAcknowledgedEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(uetr);
        Objects.requireNonNull(occurredAt);
        if (!Set.of("ACSC", "RJCT", "REVIEW").contains(outcome))
            throw new IllegalArgumentException("Invalid SWIFT outcome");
    }
}

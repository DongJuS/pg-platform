package com.shinhan.pg.common.eventcontracts;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Ledger convergence evidence. Kept separate from generic ledger.posted to distinguish hold and
 * final journals.
 */
public record OverseasLedgerAppliedEvent(
        UUID eventId,
        UUID transferId,
        String uetr,
        String outcome,
        UUID transactionId,
        UUID journalId,
        Instant occurredAt) {
    public OverseasLedgerAppliedEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(transferId);
        Objects.requireNonNull(uetr);
        Objects.requireNonNull(occurredAt);
        if (!Set.of("ACSC", "RJCT", "REVIEW").contains(outcome))
            throw new IllegalArgumentException("Invalid SWIFT outcome");
        if (!"REVIEW".equals(outcome)) {
            Objects.requireNonNull(transactionId);
            Objects.requireNonNull(journalId);
        }
    }
}

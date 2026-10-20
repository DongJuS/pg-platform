package com.shinhan.pg.wealthreport.application.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Actual event identifiers only: never infer money or account membership from these notifications.
 */
public record UpdateReportSnapshotCommand(
        String topic, UUID eventId, UUID transactionId, UUID journalId, Instant occurredAt) {
    public UpdateReportSnapshotCommand {
        if (occurredAt != null)
            occurredAt = occurredAt.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}

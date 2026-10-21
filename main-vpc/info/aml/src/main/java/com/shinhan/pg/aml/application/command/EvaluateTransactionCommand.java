package com.shinhan.pg.aml.application.command;

import java.util.UUID;

public record EvaluateTransactionCommand(
        UUID requestId, UUID eventId, UUID transactionId, String topic, UUID journalId) {
    public EvaluateTransactionCommand(UUID requestId, UUID eventId, UUID transactionId) {
        this(requestId, eventId, transactionId, "ledger.posted", null);
    }
}

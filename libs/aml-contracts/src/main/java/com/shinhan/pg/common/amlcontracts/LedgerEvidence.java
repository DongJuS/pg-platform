package com.shinhan.pg.common.amlcontracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerEvidence(
        UUID transactionId,
        UUID journalId,
        Instant postedAt,
        List<Line> lines,
        UUID overseasTransferId,
        String overseasPhase,
        String overseasOutcome) {
    public record Line(UUID accountId, BigDecimal signedAmount, String currency) {}
}

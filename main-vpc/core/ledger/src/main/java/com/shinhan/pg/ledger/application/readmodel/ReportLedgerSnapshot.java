package com.shinhan.pg.ledger.application.readmodel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Source-owned consistent committed read. Cutoff is independent of notification arrival. */
public record ReportLedgerSnapshot(
        UUID id,
        UUID userId,
        Instant from,
        Instant to,
        Instant asOf,
        Instant capturedAt,
        String sourceVersion,
        List<AccountFigures> accounts) {
    public record AccountFigures(
            UUID accountId,
            BigDecimal balance,
            BigDecimal cashIn,
            BigDecimal cashOut,
            String currency,
            long version) {}
}

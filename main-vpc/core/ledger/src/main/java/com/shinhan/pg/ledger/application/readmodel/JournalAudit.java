package com.shinhan.pg.ledger.application.readmodel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record JournalAudit(UUID transactionId, List<Line> postings) {
    public record Line(UUID accountId, BigDecimal signedAmount, String currency) {}
}

package com.shinhan.pg.payment.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** A financial effect has one stable ID across REST, webhook and ledger posting. */
public record PaymentOperation(
        UUID id,
        UUID paymentId,
        String kind,
        Money amount,
        String status,
        String pgOutcome,
        long pgVersion,
        boolean pgSent,
        boolean ledgerSent,
        UUID journalId) {
    public boolean completed() {
        return "COMPLETED".equals(status);
    }
}

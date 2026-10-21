package com.shinhan.pg.aml.domain.model;

import com.shinhan.pg.common.amlcontracts.AmlProblem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One canonical source financial effect, independent of event ID, topic and rule version. */
public record FinancialFact(
        UUID transactionId,
        UUID businessId,
        String kind,
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        UUID originalBusinessId) {
    public FinancialFact {
        AmlProblem.require(
                transactionId != null
                        && businessId != null
                        && customerId != null
                        && sourceAccountId != null
                        && amount != null
                        && amount.signum() > 0
                        && amount.precision() <= 24
                        && amount.scale() <= 4
                        && currency != null
                        && java.util.Set.of("KRW", "USD").contains(currency)
                        && occurredAt != null
                        && kind != null);
        amount = amount.stripTrailingZeros();
    }

    public boolean reversal() {
        return java.util.Set.of("PAYMENT_REFUND", "PAYMENT_CANCEL", "OVERSEAS_REVERSAL")
                .contains(kind);
    }
}

package com.shinhan.pg.common.amlcontracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Source-owned immutable evidence. Reversals refer to the original effect; holds are never new
 * remittances.
 */
public record AmlSourceFact(
        UUID transactionId,
        UUID businessId,
        String kind,
        UUID customerId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        UUID originalBusinessId,
        String status) {}

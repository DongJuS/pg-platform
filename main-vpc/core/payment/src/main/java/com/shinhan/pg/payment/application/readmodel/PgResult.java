package com.shinhan.pg.payment.application.readmodel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Provider final result, distinct from the FEP receipt. Version is scoped to operationId. */
public record PgResult(
        UUID operationId,
        UUID paymentId,
        String kind,
        String outcome,
        BigDecimal amount,
        String currency,
        long version,
        UUID eventId,
        String externalReference,
        Instant occurredAt) {}

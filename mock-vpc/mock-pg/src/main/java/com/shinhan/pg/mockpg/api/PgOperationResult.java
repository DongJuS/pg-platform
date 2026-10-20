package com.shinhan.pg.mockpg.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PgOperationResult(
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

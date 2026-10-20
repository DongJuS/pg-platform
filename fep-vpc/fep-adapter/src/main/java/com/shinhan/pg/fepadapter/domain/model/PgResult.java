package com.shinhan.pg.fepadapter.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

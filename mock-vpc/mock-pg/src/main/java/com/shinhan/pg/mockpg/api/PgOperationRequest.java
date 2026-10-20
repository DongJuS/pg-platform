package com.shinhan.pg.mockpg.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PgOperationRequest(
        UUID operationId, UUID paymentId, String kind, BigDecimal amount, String currency) {}

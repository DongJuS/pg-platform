package com.shinhan.pg.fepadapter.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record PgRequest(
        UUID operationId, UUID paymentId, String kind, BigDecimal amount, String currency) {}

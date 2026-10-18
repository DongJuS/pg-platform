package com.shinhan.pg.ledger.adapter.in.web.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** [MVP1-F11] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetBalanceResponse(
        UUID accountId, BigDecimal balance, String currency, Instant asOf) {}

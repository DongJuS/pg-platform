package com.shinhan.pg.wealthreport.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Source-local committed ledger view. No names, account numbers or restoration dictionary. */
public record AccountFigures(
        UUID accountId,
        BigDecimal balance,
        BigDecimal cashIn,
        BigDecimal cashOut,
        String currency,
        long version) {}

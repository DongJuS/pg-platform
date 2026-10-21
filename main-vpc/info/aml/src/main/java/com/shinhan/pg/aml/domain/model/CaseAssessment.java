package com.shinhan.pg.aml.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CaseAssessment(
        int riskScore,
        boolean reportRequired,
        BigDecimal totalAmount,
        int transactionCount,
        Instant windowStart,
        Instant windowEnd,
        List<String> reasons) {
    public CaseAssessment {
        reasons = List.copyOf(reasons);
    }
}

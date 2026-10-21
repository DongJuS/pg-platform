package com.shinhan.pg.aml.domain.model;

import com.shinhan.pg.common.amlcontracts.AmlProblem;

import java.math.BigDecimal;
import java.util.Set;

/** Versioned, currency-specific generic simulation rule; no regulatory threshold is built in. */
public record DetectionRule(
        String code,
        String version,
        String description,
        Integer score,
        String currency,
        Set<String> kinds,
        BigDecimal minimumAmount,
        int minimumCount,
        long windowSeconds,
        String condition) {
    public DetectionRule {
        AmlProblem.require(
                code != null
                        && code.matches("[A-Za-z0-9_.-]{1,64}")
                        && version != null
                        && version.matches("[A-Za-z0-9_.-]{1,64}")
                        && description != null
                        && description.length() <= 200
                        && score != null
                        && score > 0
                        && score <= 10000
                        && currency != null
                        && Set.of("KRW", "USD").contains(currency)
                        && kinds != null
                        && !kinds.isEmpty()
                        && Set.of("DOMESTIC_TRANSFER", "PAYMENT_APPROVAL", "OVERSEAS_TRANSFER")
                                .containsAll(kinds)
                        && minimumAmount != null
                        && minimumAmount.signum() >= 0
                        && minimumAmount.precision() <= 24
                        && minimumAmount.scale() <= 4
                        && minimumCount >= 1
                        && minimumCount <= 1000000
                        && Set.of("SINGLE_AMOUNT", "WINDOW_AMOUNT", "WINDOW_COUNT")
                                .contains(condition)
                        && windowSeconds >= 1
                        && windowSeconds <= 2678400);
        kinds = java.util.Collections.unmodifiableSet(new java.util.TreeSet<>(kinds));
        minimumAmount = minimumAmount.stripTrailingZeros();
    }

    public DetectionRule(
            String code,
            String version,
            String description,
            Integer score,
            String currency,
            Set<String> kinds,
            BigDecimal minimumAmount,
            int minimumCount,
            long windowSeconds) {
        this(
                code,
                version,
                description,
                score,
                currency,
                kinds,
                minimumAmount,
                minimumCount,
                windowSeconds,
                "WINDOW_AMOUNT");
    }
}

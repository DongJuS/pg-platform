package com.shinhan.pg.aml.adapter.out.persistence.entity;

/** PostgreSQL projection; full immutable evidence is stored separately. */
public record DetectionRuleEntity(
        String code,
        String version,
        String description,
        Integer score,
        String currency,
        java.util.Set<String> kinds,
        java.math.BigDecimal minimumAmount,
        int minimumCount,
        long windowSeconds,
        String condition) {}

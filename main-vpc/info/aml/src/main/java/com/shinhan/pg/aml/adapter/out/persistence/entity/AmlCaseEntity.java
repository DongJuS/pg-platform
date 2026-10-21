package com.shinhan.pg.aml.adapter.out.persistence.entity;

/** PostgreSQL projection; full immutable evidence is stored separately. */
public record AmlCaseEntity(
        java.util.UUID id,
        String caseKey,
        java.util.UUID transactionId,
        String ruleVersion,
        Integer riskScore,
        String status) {}

package com.shinhan.pg.aml.adapter.out.persistence.entity;

/** PostgreSQL projection; full immutable evidence is stored separately. */
public record AmlReportEntity(
        java.util.UUID id,
        java.util.UUID caseId,
        String reportKey,
        String reportType,
        String status,
        String receiptReference) {}

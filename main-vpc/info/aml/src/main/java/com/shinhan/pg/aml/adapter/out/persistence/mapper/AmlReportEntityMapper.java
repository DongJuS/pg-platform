package com.shinhan.pg.aml.adapter.out.persistence.mapper;

import com.shinhan.pg.aml.adapter.out.persistence.entity.AmlReportEntity;
import com.shinhan.pg.aml.domain.model.AmlReport;

public final class AmlReportEntityMapper {
    public AmlReport toDomain(AmlReportEntity e) {
        return new AmlReport(
                e.id(),
                e.caseId(),
                e.reportKey(),
                e.reportType(),
                e.status(),
                e.receiptReference());
    }

    public AmlReportEntity toEntity(AmlReport v) {
        return new AmlReportEntity(
                v.id(),
                v.caseId(),
                v.reportKey(),
                v.reportType(),
                v.status(),
                v.receiptReference());
    }
}

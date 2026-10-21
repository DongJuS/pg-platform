package com.shinhan.pg.aml.adapter.out.persistence.mapper;

import com.shinhan.pg.aml.adapter.out.persistence.entity.AmlCaseEntity;
import com.shinhan.pg.aml.domain.model.AmlCase;

public final class AmlCaseEntityMapper {
    public AmlCase toDomain(AmlCaseEntity e) {
        return new AmlCase(
                e.id(), e.caseKey(), e.transactionId(), e.ruleVersion(), e.riskScore(), e.status());
    }

    public AmlCaseEntity toEntity(AmlCase v) {
        return new AmlCaseEntity(
                v.id(), v.caseKey(), v.transactionId(), v.ruleVersion(), v.riskScore(), v.status());
    }
}

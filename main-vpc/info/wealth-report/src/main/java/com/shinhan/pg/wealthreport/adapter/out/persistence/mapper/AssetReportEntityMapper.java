package com.shinhan.pg.wealthreport.adapter.out.persistence.mapper;

import com.shinhan.pg.wealthreport.adapter.out.persistence.entity.AssetReportEntity;
import com.shinhan.pg.wealthreport.domain.model.AssetReport;

public final class AssetReportEntityMapper {
    public AssetReport toDomain(AssetReportEntity e) {
        return new AssetReport(
                e.id(),
                e.userId(),
                e.asOf(),
                e.status(),
                e.stage(),
                e.calculationVersion(),
                e.failureCode(),
                e.draftSource(),
                e.completedAt());
    }

    public AssetReportEntity toEntity(AssetReport e) {
        return new AssetReportEntity(
                e.id(),
                e.userId(),
                e.asOf(),
                e.status(),
                e.stage(),
                e.calculationVersion(),
                e.failureCode(),
                e.draftSource(),
                e.completedAt());
    }
}

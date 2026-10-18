package com.shinhan.pg.wealthreport.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.adapter.out.persistence.entity.AssetReportEntity;
import com.shinhan.pg.wealthreport.domain.model.AssetReport;

/** [MVP4-F01,MVP4-F02,MVP4-F03,MVP4-F10] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class AssetReportEntityMapper {
    public AssetReport toDomain(AssetReportEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP4-F01");
    }

    public AssetReportEntity toEntity(AssetReport value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP4-F01");
    }
}

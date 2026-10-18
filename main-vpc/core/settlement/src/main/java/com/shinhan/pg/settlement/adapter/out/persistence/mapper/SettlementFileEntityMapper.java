package com.shinhan.pg.settlement.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.settlement.adapter.out.persistence.entity.SettlementFileEntity;
import com.shinhan.pg.settlement.domain.model.SettlementFile;

/** [MVP7-F02,MVP7-F03,MVP7-F07] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class SettlementFileEntityMapper {
    public SettlementFile toDomain(SettlementFileEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP7-F02");
    }

    public SettlementFileEntity toEntity(SettlementFile value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP7-F02");
    }
}

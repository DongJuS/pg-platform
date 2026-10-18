package com.shinhan.pg.aml.adapter.out.persistence.mapper;

import com.shinhan.pg.aml.adapter.out.persistence.entity.AmlCaseEntity;
import com.shinhan.pg.aml.domain.model.AmlCase;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** [MVP6-F01,MVP6-F02,MVP6-F04] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class AmlCaseEntityMapper {
    public AmlCase toDomain(AmlCaseEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP6-F01");
    }

    public AmlCaseEntity toEntity(AmlCase value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP6-F01");
    }
}

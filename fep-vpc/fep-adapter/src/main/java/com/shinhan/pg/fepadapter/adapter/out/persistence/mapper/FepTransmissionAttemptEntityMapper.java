package com.shinhan.pg.fepadapter.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.fepadapter.adapter.out.persistence.entity.FepTransmissionAttemptEntity;
import com.shinhan.pg.fepadapter.domain.model.FepTransmissionAttempt;

/** [MVP5-F04,MVP7-F03] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class FepTransmissionAttemptEntityMapper {
    public FepTransmissionAttempt toDomain(FepTransmissionAttemptEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP5-F04");
    }

    public FepTransmissionAttemptEntity toEntity(FepTransmissionAttempt value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP5-F04");
    }
}

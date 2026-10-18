package com.shinhan.pg.swift.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.swift.adapter.out.persistence.entity.SwiftMessageEntity;
import com.shinhan.pg.swift.domain.model.SwiftMessage;

/** [MVP5-F03,MVP5-F05,MVP5-F08] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class SwiftMessageEntityMapper {
    public SwiftMessage toDomain(SwiftMessageEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP5-F03");
    }

    public SwiftMessageEntity toEntity(SwiftMessage value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP5-F03");
    }
}

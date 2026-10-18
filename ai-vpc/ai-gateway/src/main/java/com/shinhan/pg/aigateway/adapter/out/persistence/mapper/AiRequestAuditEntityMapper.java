package com.shinhan.pg.aigateway.adapter.out.persistence.mapper;

import com.shinhan.pg.aigateway.adapter.out.persistence.entity.AiRequestAuditEntity;
import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** [MVP3-F01,MVP3-F02] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class AiRequestAuditEntityMapper {
    public AiRequestAudit toDomain(AiRequestAuditEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP3-F01");
    }

    public AiRequestAuditEntity toEntity(AiRequestAudit value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP3-F01");
    }
}

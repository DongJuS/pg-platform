package com.shinhan.pg.payment.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.adapter.out.persistence.entity.ReconciliationRunEntity;
import com.shinhan.pg.payment.domain.model.ReconciliationRun;

/** [MVP2-F08,MVP2-F09] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class ReconciliationRunEntityMapper {
    public ReconciliationRun toDomain(ReconciliationRunEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP2-F08");
    }

    public ReconciliationRunEntity toEntity(ReconciliationRun value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP2-F08");
    }
}

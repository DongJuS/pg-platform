package com.shinhan.pg.ledger.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.ledger.adapter.out.persistence.entity.LedgerAccountEntity;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

/** [MVP1-F11,MVP1-F19] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class LedgerAccountEntityMapper {
    public LedgerAccount toDomain(LedgerAccountEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP1-F11");
    }

    public LedgerAccountEntity toEntity(LedgerAccount value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP1-F11");
    }
}

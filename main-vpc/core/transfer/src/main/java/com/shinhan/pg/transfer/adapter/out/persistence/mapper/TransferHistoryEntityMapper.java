package com.shinhan.pg.transfer.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.adapter.out.persistence.entity.TransferHistoryEntity;
import com.shinhan.pg.transfer.domain.model.TransferHistory;

/** [MVP1-F16] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class TransferHistoryEntityMapper {
    public TransferHistory toDomain(TransferHistoryEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP1-F16");
    }

    public TransferHistoryEntity toEntity(TransferHistory value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP1-F16");
    }
}

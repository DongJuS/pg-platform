package com.shinhan.pg.auth.adapter.out.persistence.mapper;

import com.shinhan.pg.auth.adapter.out.persistence.entity.RefreshSessionEntity;
import com.shinhan.pg.auth.domain.model.RefreshSession;

/** [MVP1-F03] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class RefreshSessionEntityMapper {
    public RefreshSession toDomain(RefreshSessionEntity entity) {
        return new RefreshSession(
                entity.id(),
                entity.userId(),
                entity.tokenHash(),
                entity.familyId(),
                entity.expiresAt(),
                entity.revokedAt());
    }

    public RefreshSessionEntity toEntity(RefreshSession value) {
        return new RefreshSessionEntity(
                value.id(),
                value.userId(),
                value.tokenHash(),
                value.familyId(),
                value.expiresAt(),
                value.revokedAt());
    }
}

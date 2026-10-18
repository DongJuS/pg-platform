package com.shinhan.pg.auth.adapter.out.persistence.mapper;

import com.shinhan.pg.auth.adapter.out.persistence.entity.OtpProofEntity;
import com.shinhan.pg.auth.domain.model.OtpProof;

/** [MVP1-F06] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class OtpProofEntityMapper {
    public OtpProof toDomain(OtpProofEntity entity) {
        return new OtpProof(
                entity.id(),
                entity.userId(),
                entity.transferDraftId(),
                entity.proofHash(),
                entity.expiresAt(),
                entity.consumedAt(),
                entity.failureCount());
    }

    public OtpProofEntity toEntity(OtpProof value) {
        return new OtpProofEntity(
                value.id(),
                value.userId(),
                value.transferDraftId(),
                value.proofHash(),
                value.expiresAt(),
                value.consumedAt(),
                value.failureCount());
    }
}

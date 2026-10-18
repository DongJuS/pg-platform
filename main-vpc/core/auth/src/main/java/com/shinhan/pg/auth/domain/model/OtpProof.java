package com.shinhan.pg.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F06] Mock OTP 검증 시도 기록. 일회 소모·만료·실패 잠금을 저장한다. 실패 횟수는 계정이 아니라 이체 초안 단위로 누적한다. */
public record OtpProof(
        UUID id,
        UUID userId,
        UUID transferDraftId,
        String proofHash,
        Instant expiresAt,
        Instant consumedAt,
        int failureCount) {
    public OtpProof {
        if (failureCount < 0) {
            throw new IllegalArgumentException("failureCount must not be negative");
        }
    }

    public OtpProof withFailure(int count) {
        return new OtpProof(id, userId, transferDraftId, proofHash, expiresAt, consumedAt, count);
    }

    public OtpProof consumed(Instant at) {
        return new OtpProof(id, userId, transferDraftId, proofHash, expiresAt, at, failureCount);
    }
}

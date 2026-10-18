package com.shinhan.pg.auth.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F06] otp_proofs 테이블 매핑. OTP 코드 원문이 아니라 검증 성공 시 소모 기록의 해시만 저장한다. */
public final class OtpProofEntity {
    private final UUID id;
    private final UUID userId;
    private final UUID transferDraftId;
    private final String proofHash;
    private final Instant expiresAt;
    private final Instant consumedAt;
    private final int failureCount;

    public OtpProofEntity(
            UUID id,
            UUID userId,
            UUID transferDraftId,
            String proofHash,
            Instant expiresAt,
            Instant consumedAt,
            int failureCount) {
        this.id = id;
        this.userId = userId;
        this.transferDraftId = transferDraftId;
        this.proofHash = proofHash;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
        this.failureCount = failureCount;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID transferDraftId() {
        return transferDraftId;
    }

    public String proofHash() {
        return proofHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    public int failureCount() {
        return failureCount;
    }
}

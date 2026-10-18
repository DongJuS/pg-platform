package com.shinhan.pg.auth.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F03] refresh_sessions 테이블 매핑. 토큰 원문 대신 SHA-256 해시만 저장한다. */
public final class RefreshSessionEntity {
    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private final Instant expiresAt;
    private final Instant revokedAt;

    public RefreshSessionEntity(
            UUID id,
            UUID userId,
            String tokenHash,
            UUID familyId,
            Instant expiresAt,
            Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UUID familyId() {
        return familyId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}

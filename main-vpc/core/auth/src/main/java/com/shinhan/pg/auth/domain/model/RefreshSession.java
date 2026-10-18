package com.shinhan.pg.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F03] 순수 업무 모델 초안. 회전·재사용 감지의 원자성을 정의한다. 기본 로그아웃의 refresh 전체 철회는 보류다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record RefreshSession(
        UUID id,
        UUID userId,
        String tokenHash,
        UUID familyId,
        Instant expiresAt,
        Instant revokedAt) {
    @Override
    public String toString() {
        return "RefreshSession[REDACTED]";
    }
}

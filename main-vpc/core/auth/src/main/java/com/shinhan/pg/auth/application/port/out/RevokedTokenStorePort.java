package com.shinhan.pg.auth.application.port.out;

import java.time.Instant;

/** [MVP1-F04] 기본 로그아웃의 access jti를 남은 만료 시간만큼 철회한다. Redis TTL·장애 계약은 후속. */
public interface RevokedTokenStorePort {
    void revokeUntilExpiry(String jti, Instant expiresAt);
}

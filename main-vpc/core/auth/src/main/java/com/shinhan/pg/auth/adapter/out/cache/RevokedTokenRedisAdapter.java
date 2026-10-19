package com.shinhan.pg.auth.adapter.out.cache;

import com.shinhan.pg.auth.application.port.out.RevokedTokenStorePort;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

/** [MVP1-F04] access 토큰 jti를 남은 만료 시간만큼 Redis에 철회한다. Redis 장애 시 로그아웃은 실패로 둔다. */
public final class RevokedTokenRedisAdapter implements RevokedTokenStorePort {
    private final String keyPrefix;

    private final StringRedisTemplate redis;

    public RevokedTokenRedisAdapter(StringRedisTemplate redis) {
        this(redis, "auth:revoked:jti:");
    }

    public RevokedTokenRedisAdapter(StringRedisTemplate redis, String keyPrefix) {
        this.redis = java.util.Objects.requireNonNull(redis);
        if (keyPrefix == null || keyPrefix.isBlank()) throw new IllegalArgumentException("Revocation prefix is required");
        this.keyPrefix = keyPrefix;
    }

    @Override
    public void revokeUntilExpiry(String jti, Instant expiresAt) {
        var ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(keyPrefix + jti, "1", ttl);
    }
}

package com.shinhan.pg.auth.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F02,MVP1-F03] 자체 RS256 access 토큰 발급·검증. refresh는 불투명 토큰으로 별도 저장소에서 검증한다. */
public interface TokenIssuancePort {
    IssuedAccessToken issueAccessToken(UUID userId);

    /** 서명·만료·issuer·audience를 모두 검증한다. 실패 시 예외가 나간다. */
    VerifiedAccessToken verifyAccessToken(String token);

    /** 이미 신뢰한 문맥의 토큰에서 jti·만료만 읽는다(로그아웃 등). 서명 검증은 하지 않는다. */
    UnverifiedAccessToken parseUnverified(String token);

    record IssuedAccessToken(String token, String jti, Instant expiresAt) {
        @Override
        public String toString() {
            return "IssuedAccessToken[REDACTED]";
        }
    }

    record VerifiedAccessToken(UUID userId, String jti, Instant expiresAt) {}

    record UnverifiedAccessToken(String jti, Instant expiresAt) {}
}

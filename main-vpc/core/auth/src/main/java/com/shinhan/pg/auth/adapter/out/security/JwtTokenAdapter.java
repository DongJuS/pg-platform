package com.shinhan.pg.auth.adapter.out.security;

import com.shinhan.pg.auth.application.port.out.TokenIssuancePort;

import java.time.Duration;
import java.util.UUID;

/** [MVP1-F02,MVP1-F03] RS256 키·토큰 수명·검증 구현. 키 원문·토큰 로그 금지. */
public final class JwtTokenAdapter implements TokenIssuancePort {
    private final Rs256JwtSupport jwt;
    private final String issuer;
    private final String audience;
    private final Duration accessTtl;

    public JwtTokenAdapter(
            Rs256JwtSupport jwt, String issuer, String audience, Duration accessTtl) {
        this.jwt = java.util.Objects.requireNonNull(jwt);
        this.issuer = java.util.Objects.requireNonNull(issuer);
        this.audience = java.util.Objects.requireNonNull(audience);
        this.accessTtl = java.util.Objects.requireNonNull(accessTtl);
    }

    @Override
    public IssuedAccessToken issueAccessToken(UUID userId) {
        var token = jwt.issue(userId, issuer, audience, new Rs256JwtSupport.DurationSpec(accessTtl));
        var claims = jwt.parseUnverified(token);
        return new IssuedAccessToken(token, claims.jti(), claims.expiresAt());
    }

    @Override
    public VerifiedAccessToken verifyAccessToken(String token) {
        var verified = jwt.verify(token, issuer, audience);
        return new VerifiedAccessToken(verified.userId(), verified.jti(), verified.expiresAt());
    }

    @Override
    public UnverifiedAccessToken parseUnverified(String token) {
        var claims = jwt.parseUnverified(token);
        return new UnverifiedAccessToken(claims.jti(), claims.expiresAt());
    }
}

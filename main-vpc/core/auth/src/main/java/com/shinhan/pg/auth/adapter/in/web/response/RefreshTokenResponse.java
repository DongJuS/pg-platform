package com.shinhan.pg.auth.adapter.in.web.response;

import java.time.Instant;

/** [MVP1-F03] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RefreshTokenResponse(
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt,
        Instant refreshExpiresAt) {
    @Override
    public String toString() {
        return "RefreshTokenResponse[REDACTED]";
    }
}

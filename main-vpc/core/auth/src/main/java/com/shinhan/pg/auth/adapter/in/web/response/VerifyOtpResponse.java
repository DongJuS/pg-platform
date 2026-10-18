package com.shinhan.pg.auth.adapter.in.web.response;

import java.time.Instant;

/** [MVP1-F06] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record VerifyOtpResponse(String otpToken, Instant expiresAt) {
    @Override
    public String toString() {
        return "VerifyOtpResponse[REDACTED]";
    }
}

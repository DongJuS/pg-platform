package com.shinhan.pg.auth.application.result;

import java.time.Instant;

/** [MVP1-F06] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record VerifyOtpResult(String otpToken, Instant expiresAt) {
    @Override
    public String toString() {
        return "VerifyOtpResult[REDACTED]";
    }
}

package com.shinhan.pg.auth.domain.security;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * [MVP1-F06] Mock OTP — RFC 6238 TOTP(HmacSHA1, 30초 단계, 6자리) 구현.
 * 실제 OTP 제공사를 호출하지 않는다. 사용자별 시드는 서버 비밀과 사용자 ID의
 * HMAC으로 도출하므로 클라이언트 등록 절차 없이 Mock 검증이 가능하다.
 */
public final class TotpMock {
    public static final Duration TIME_STEP = Duration.ofSeconds(30);
    public static final int DIGITS = 6;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private TotpMock() {}

    /** 사용자별 TOTP 시드 = HMAC-SHA256(serverSecret, "otp-seed:" + userId)의 앞 20바이트. */
    public static byte[] seedFor(byte[] serverSecret, java.util.UUID userId) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(serverSecret, "HmacSHA256"));
            var derived = mac.doFinal(("otp-seed:" + userId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var seed = new byte[20];
            System.arraycopy(derived, 0, seed, 0, seed.length);
            return seed;
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("OTP seed derivation failed", error);
        }
    }

    /** 주어진 시각의 TOTP를 계산한다. 테스트와 서버 검증이 동일한 계산을 쓴다. */
    public static String codeAt(byte[] seed, Instant instant) {
        return codeForCounter(seed, instant.getEpochSecond() / TIME_STEP.getSeconds());
    }

    /** 현재 시각 기준 ±allowedWindow 단계 안의 코드와 상수 시간 비교로 검증한다. */
    public static boolean verify(byte[] seed, String submittedCode, Instant instant, int allowedWindow) {
        if (submittedCode == null || !submittedCode.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        var counter = instant.getEpochSecond() / TIME_STEP.getSeconds();
        for (long offset = -allowedWindow; offset <= allowedWindow; offset++) {
            var expected = codeForCounter(seed, counter + offset);
            if (MessageDigest.isEqual(
                    expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    submittedCode.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
                return true;
            }
        }
        return false;
    }

    private static String codeForCounter(byte[] seed, long counter) {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(seed, HMAC_ALGORITHM));
            var hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());
            var offset = hash[hash.length - 1] & 0x0f;
            var binary =
                    ((hash[offset] & 0x7f) << 24)
                            | ((hash[offset + 1] & 0xff) << 16)
                            | ((hash[offset + 2] & 0xff) << 8)
                            | (hash[offset + 3] & 0xff);
            var otp = binary % (int) Math.pow(10, DIGITS);
            return java.lang.String.format("%0" + DIGITS + "d", otp);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("TOTP generation failed", error);
        }
    }
}

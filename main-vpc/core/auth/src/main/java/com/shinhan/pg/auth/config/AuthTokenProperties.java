package com.shinhan.pg.auth.config;

import java.time.Duration;

/**
 * [MVP1-F02,MVP1-F03] 자체 발급 JWT·OTP 설정. 값은 환경 변수/속성으로 주입하고
 * 개발 기본값은 로컬에서만 사용한다. 키 원문은 로그에 남기지 않는다.
 * pg.auth.jwt.private-key-pem / public-key-pem 이 없으면 서버는 개발용 임시 키를 생성한다.
 */
public record AuthTokenProperties(
        String issuer,
        String audience,
        String keyId,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration otpTokenTtl,
        String privateKeyPem,
        String publicKeyPem,
        byte[] otpServerSecret) {

    public static AuthTokenProperties defaults() {
        return new AuthTokenProperties(
                "pg-platform-auth",
                "pg-platform",
                "dev-1",
                Duration.ofMinutes(15),
                Duration.ofDays(14),
                Duration.ofMinutes(5),
                null,
                null,
                randomOtpSecret());
    }

    public static AuthTokenProperties from(org.springframework.core.env.Environment environment) {
        var defaults = defaults();
        var secretHex = environment.getProperty("pg.auth.otp-server-secret-hex");
        return new AuthTokenProperties(
                environment.getProperty("pg.auth.jwt.issuer", defaults.issuer()),
                environment.getProperty("pg.auth.jwt.audience", defaults.audience()),
                environment.getProperty("pg.auth.jwt.key-id", defaults.keyId()),
                Duration.parse(environment.getProperty("pg.auth.jwt.access-ttl", "PT15M")),
                Duration.parse(environment.getProperty("pg.auth.jwt.refresh-ttl", "P14D")),
                Duration.parse(environment.getProperty("pg.auth.otp.token-ttl", "PT5M")),
                environment.getProperty("pg.auth.jwt.private-key-pem"),
                environment.getProperty("pg.auth.jwt.public-key-pem"),
                secretHex != null && !secretHex.isBlank() ? hexDecode(secretHex) : defaults.otpServerSecret());
    }

    private static byte[] randomOtpSecret() {
        var bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static byte[] hexDecode(String hex) {
        if (!hex.matches("(?:[0-9a-fA-F]{2}){16,}")) throw new IllegalArgumentException("OTP secret must contain at least 16 bytes of hexadecimal data");
        var out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}

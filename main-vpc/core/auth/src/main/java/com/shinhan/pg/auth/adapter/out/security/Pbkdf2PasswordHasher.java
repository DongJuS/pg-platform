package com.shinhan.pg.auth.adapter.out.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * [MVP1-F01,MVP1-F02] PBKDF2-HMAC-SHA256 비밀번호 해시. spring-security-crypto 미사용 —
 * JDK 표준만으로 구현한다. 저장 형식: pbkdf2$<iterations>$<salt b64>$<hash b64>.
 * 평문 비밀번호와 해시 원문을 로그에 남기지 않는다.
 * 반복 횟수는 OWASP PBKDF2-HMAC-SHA256 권고(600,000)를 따른다.
 */
public final class Pbkdf2PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        var salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        var derived = derive(rawPassword.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$"
                + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        var parts = storedHash.split("\\$");
        if (parts.length != 4 || !"pbkdf2".equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException invalid) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        var actual = derive(rawPassword.toCharArray(), salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            var factory = SecretKeyFactory.getInstance(ALGORITHM);
            var spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException error) {
            throw new IllegalStateException("PBKDF2 derivation failed", error);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}

package com.shinhan.pg.auth.adapter.out.security;

import com.shinhan.pg.auth.application.port.out.PasswordHashPort;

/** [MVP1-F01,MVP1-F02] PBKDF2-HMAC-SHA256 해시/검증. 평문 비밀번호를 저장·로그하지 않는다. */
public final class PasswordHashAdapter implements PasswordHashPort {
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

    @Override
    public String hash(String rawPassword) {
        return hasher.hash(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return hasher.matches(rawPassword, storedHash);
    }
}

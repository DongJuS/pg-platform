package com.shinhan.pg.auth.application.port.out;

/** [MVP1-F01,MVP1-F02] BCrypt 해시/검증 위치. 평문 비밀번호를 저장·로그하지 않는다. */
public interface PasswordHashPort {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String storedHash);
}

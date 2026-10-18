package com.shinhan.pg.auth.domain.model;

import java.util.List;
import java.util.UUID;

/** [MVP1-F01,MVP1-F02,MVP1-F05] 순수 업무 모델 초안. 이메일 고유성·비밀번호 해시만 저장. 이메일/전화번호/해시를 로그에 남기지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record User(
        UUID id,
        String email,
        String passwordHash,
        String name,
        String phoneNumber,
        List<String> roles) {
    @Override
    public String toString() {
        return "User[REDACTED]";
    }
}

package com.shinhan.pg.auth.adapter.out.persistence.entity;

import java.util.List;
import java.util.UUID;

/** [MVP1-F01,MVP1-F02,MVP1-F05] users 테이블 매핑. roles는 쉼표 구분 문자열로 저장한다. */
public final class UserEntity {
    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String name;
    private final String phoneNumber;
    private final List<String> roles;

    public UserEntity(
            UUID id,
            String email,
            String passwordHash,
            String name,
            String phoneNumber,
            List<String> roles) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.roles = List.copyOf(roles);
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String name() {
        return name;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public List<String> roles() {
        return roles;
    }
}

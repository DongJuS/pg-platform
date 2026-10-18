package com.shinhan.pg.auth.adapter.out.persistence.mapper;

import com.shinhan.pg.auth.adapter.out.persistence.entity.UserEntity;
import com.shinhan.pg.auth.domain.model.User;

import java.util.Arrays;
import java.util.List;

/** [MVP1-F01,MVP1-F02,MVP1-F05] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class UserEntityMapper {
    public User toDomain(UserEntity entity) {
        return new User(
                entity.id(),
                entity.email(),
                entity.passwordHash(),
                entity.name(),
                entity.phoneNumber(),
                entity.roles());
    }

    public UserEntity toEntity(User value) {
        return new UserEntity(
                value.id(),
                value.email(),
                value.passwordHash(),
                value.name(),
                value.phoneNumber(),
                value.roles());
    }

    public String rolesToColumn(List<String> roles) {
        return String.join(",", roles);
    }

    public List<String> columnToRoles(String column) {
        if (column == null || column.isBlank()) {
            return List.of();
        }
        return Arrays.asList(column.split(","));
    }
}

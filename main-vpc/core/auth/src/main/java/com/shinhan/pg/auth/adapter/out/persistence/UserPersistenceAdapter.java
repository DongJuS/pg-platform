package com.shinhan.pg.auth.adapter.out.persistence;

import com.shinhan.pg.auth.adapter.out.persistence.entity.UserEntity;
import com.shinhan.pg.auth.adapter.out.persistence.mapper.UserEntityMapper;
import com.shinhan.pg.auth.application.port.out.UserRepository;
import com.shinhan.pg.auth.domain.model.User;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * [MVP1-F01,MVP1-F02,MVP1-F05] users 테이블 JDBC 영속성.
 * 이메일·비밀번호 해시·개인정보를 로그에 남기지 않는다.
 */
public final class UserPersistenceAdapter implements UserRepository {
    private final JdbcTemplate jdbc;
    private final UserEntityMapper mapper = new UserEntityMapper();

    public UserPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbc.query(
                        "select id, email, password_hash, name, phone_number, roles"
                                + " from users where id = ?",
                        rowMapper(), id.toString())
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public void save(User value) {
        var entity = mapper.toEntity(value);
        jdbc.update(
                "insert into users (id, email, password_hash, name, phone_number, roles)"
                        + " values (?, ?, ?, ?, ?, ?)"
                        + " on conflict (id) do update set"
                        + " email = excluded.email,"
                        + " password_hash = excluded.password_hash,"
                        + " name = excluded.name,"
                        + " phone_number = excluded.phone_number,"
                        + " roles = excluded.roles",
                entity.id().toString(),
                entity.email(),
                entity.passwordHash(),
                entity.name(),
                entity.phoneNumber(),
                mapper.rolesToColumn(entity.roles()));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.query(
                        "select id, email, password_hash, name, phone_number, roles"
                                + " from users where email = ?",
                        rowMapper(), email)
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    private org.springframework.jdbc.core.RowMapper<UserEntity> rowMapper() {
        return (row, index) ->
                new UserEntity(
                        UUID.fromString(row.getString("id")),
                        row.getString("email"),
                        row.getString("password_hash"),
                        row.getString("name"),
                        row.getString("phone_number"),
                        mapper.columnToRoles(row.getString("roles")));
    }
}

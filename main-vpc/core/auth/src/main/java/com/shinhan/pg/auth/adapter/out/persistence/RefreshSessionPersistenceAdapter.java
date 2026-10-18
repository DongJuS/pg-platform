package com.shinhan.pg.auth.adapter.out.persistence;

import com.shinhan.pg.auth.adapter.out.persistence.entity.RefreshSessionEntity;
import com.shinhan.pg.auth.adapter.out.persistence.mapper.RefreshSessionEntityMapper;
import com.shinhan.pg.auth.application.port.out.RefreshSessionRepository;
import com.shinhan.pg.auth.domain.model.RefreshSession;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** [MVP1-F03] refresh_sessions 테이블 JDBC 영속성. 토큰 원문은 저장하지 않는다. */
public class RefreshSessionPersistenceAdapter implements RefreshSessionRepository {
    private final JdbcTemplate jdbc;
    private final RefreshSessionEntityMapper mapper = new RefreshSessionEntityMapper();

    public RefreshSessionPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RefreshSession> findById(UUID id) {
        return jdbc.query(
                        "select id, user_id, token_hash, family_id, expires_at, revoked_at"
                                + " from refresh_sessions where id = ?",
                        rowMapper(), id.toString())
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public void save(RefreshSession value) {
        var entity = mapper.toEntity(value);
        jdbc.update(
                "insert into refresh_sessions (id, user_id, token_hash, family_id, expires_at, revoked_at)"
                        + " values (?, ?, ?, ?, ?, ?)"
                        + " on conflict (id) do update set"
                        + " user_id = excluded.user_id,"
                        + " token_hash = excluded.token_hash,"
                        + " family_id = excluded.family_id,"
                        + " expires_at = excluded.expires_at,"
                        + " revoked_at = excluded.revoked_at",
                entity.id().toString(),
                entity.userId().toString(),
                entity.tokenHash(),
                entity.familyId().toString(),
                Timestamp.from(entity.expiresAt()),
                entity.revokedAt() == null ? null : Timestamp.from(entity.revokedAt()));
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return jdbc.query(
                        "select id, user_id, token_hash, family_id, expires_at, revoked_at"
                                + " from refresh_sessions where token_hash = ?",
                        rowMapper(), tokenHash)
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    /** 재사용 감지로 인한 예외 롤백 뒤에도 family 철회가 남아 있어야 하므로 별도 트랜잭션으로 커밋한다. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID familyId, Instant revokedAt) {
        jdbc.update(
                "update refresh_sessions set revoked_at = ?"
                        + " where family_id = ? and revoked_at is null",
                Timestamp.from(revokedAt), familyId.toString());
    }

    private org.springframework.jdbc.core.RowMapper<RefreshSessionEntity> rowMapper() {        return (row, index) -> {
            var revoked = row.getTimestamp("revoked_at");
            return new RefreshSessionEntity(
                    UUID.fromString(row.getString("id")),
                    UUID.fromString(row.getString("user_id")),
                    row.getString("token_hash"),
                    UUID.fromString(row.getString("family_id")),
                    row.getTimestamp("expires_at").toInstant(),
                    revoked == null ? null : revoked.toInstant());
        };
    }
}

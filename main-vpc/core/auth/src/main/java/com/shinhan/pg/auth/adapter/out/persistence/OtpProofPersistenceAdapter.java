package com.shinhan.pg.auth.adapter.out.persistence;

import com.shinhan.pg.auth.adapter.out.persistence.entity.OtpProofEntity;
import com.shinhan.pg.auth.adapter.out.persistence.mapper.OtpProofEntityMapper;
import com.shinhan.pg.auth.application.port.out.OtpProofRepository;
import com.shinhan.pg.auth.domain.model.OtpProof;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

/** [MVP1-F06] otp_proofs 테이블 JDBC 영속성. 실패 횟수·소모 시각을 원자적으로 갱신한다. */
public class OtpProofPersistenceAdapter implements OtpProofRepository {
    private final JdbcTemplate jdbc;
    private final OtpProofEntityMapper mapper = new OtpProofEntityMapper();

    public OtpProofPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<OtpProof> findById(UUID id) {
        return jdbc.query(
                        "select id, user_id, transfer_draft_id, proof_hash, expires_at, consumed_at, failure_count"
                                + " from otp_proofs where id = ?",
                        rowMapper(), id.toString())
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    /** 실패 누적·소모 증적은 OTP_INVALID 등의 예외 롤백 뒤에도 남아 있어야 하므로 별도 트랜잭션으로 커밋한다. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(OtpProof value) {
        var entity = mapper.toEntity(value);
        jdbc.update(
                "insert into otp_proofs (id, user_id, transfer_draft_id, proof_hash, expires_at, consumed_at, failure_count)"
                        + " values (?, ?, ?, ?, ?, ?, ?)"
                        + " on conflict (id) do update set"
                        + " user_id = excluded.user_id,"
                        + " transfer_draft_id = excluded.transfer_draft_id,"
                        + " proof_hash = excluded.proof_hash,"
                        + " expires_at = excluded.expires_at,"
                        + " consumed_at = excluded.consumed_at,"
                        + " failure_count = excluded.failure_count",
                entity.id().toString(),
                entity.userId().toString(),
                entity.transferDraftId().toString(),
                entity.proofHash(),
                Timestamp.from(entity.expiresAt()),
                entity.consumedAt() == null ? null : Timestamp.from(entity.consumedAt()),
                entity.failureCount());
    }

    @Override
    public Optional<OtpProof> findLatestByUserIdAndTransferDraftId(UUID userId, UUID transferDraftId) {
        return jdbc.query(
                        "select id, user_id, transfer_draft_id, proof_hash, expires_at, consumed_at, failure_count"
                                + " from otp_proofs where user_id = ? and transfer_draft_id = ?"
                                + " order by created_at desc limit 1",
                        rowMapper(), userId.toString(), transferDraftId.toString())
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public boolean useForTransfer(UUID proofId, UUID userId, String idempotencyKey, String requestHash, java.time.Instant now) {
        int created = jdbc.update(
            "insert into otp_transfer_uses(proof_id,idempotency_key,request_hash,consumed_at)"
            + " select id,?,?,? from otp_proofs where id=? and user_id=?"
            + " and consumed_at is not null and failure_count=0 and expires_at>?"
            + " on conflict (proof_id) do nothing",
            idempotencyKey, requestHash, Timestamp.from(now), proofId.toString(), userId.toString(), Timestamp.from(now));
        if (created == 1) return true;
        return !jdbc.query("select u.proof_id from otp_transfer_uses u join otp_proofs p on p.id=u.proof_id"
            + " where u.proof_id=? and p.user_id=? and p.expires_at>? and u.idempotency_key=? and u.request_hash=?",
            (row,index) -> row.getString(1), proofId.toString(), userId.toString(), Timestamp.from(now), idempotencyKey, requestHash).isEmpty();
    }

    private org.springframework.jdbc.core.RowMapper<OtpProofEntity> rowMapper() {
        return (row, index) -> {
            var consumed = row.getTimestamp("consumed_at");
            return new OtpProofEntity(
                    UUID.fromString(row.getString("id")),
                    UUID.fromString(row.getString("user_id")),
                    UUID.fromString(row.getString("transfer_draft_id")),
                    row.getString("proof_hash"),
                    row.getTimestamp("expires_at").toInstant(),
                    consumed == null ? null : consumed.toInstant(),
                    row.getInt("failure_count"));
        };
    }
}

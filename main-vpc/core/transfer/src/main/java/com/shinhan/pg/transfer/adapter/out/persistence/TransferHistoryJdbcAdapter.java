package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.transfer.application.port.out.TransferHistoryRepository;
import com.shinhan.pg.transfer.domain.model.TransferHistory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F16] TransferHistory 영속성 구현 위치.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 *
 * <p>WO-A6 실구현: JdbcTemplate 기반. 상태 전이 이력의 원자 기록은 TransferAtomicWriteAdapter가
 * 같은 로컬 트랜잭션에서 수행하며, 이 어댑터의 save는 조회/보조 경로용이다.</p>
 */
public final class TransferHistoryJdbcAdapter implements TransferHistoryRepository {
    private final JdbcTemplate jdbc;

    public TransferHistoryJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    private final RowMapper<TransferHistory> mapper =
            (row, index) ->
                    new TransferHistory(
                            UUID.fromString(row.getString("id")),
                            UUID.fromString(row.getString("transfer_id")),
                            row.getString("previous_status"),
                            row.getString("next_status"),
                            row.getString("reason_code"),
                            row.getTimestamp("occurred_at").toInstant());

    @Override
    public Optional<TransferHistory> findById(UUID id) {
        return jdbc.query(
                        "select id, transfer_id, previous_status, next_status, reason_code,"
                                + " occurred_at from transfer_histories where id = ?",
                        mapper,
                        id.toString())
                .stream()
                .findFirst();
    }

    @Override
    public void save(TransferHistory value) {
        jdbc.update(
                "insert into transfer_histories"
                        + " (id, transfer_id, previous_status, next_status, reason_code, occurred_at)"
                        + " values (?, ?, ?, ?, ?, ?)",
                value.id().toString(),
                value.transferId().toString(),
                value.previousStatus(),
                value.nextStatus(),
                value.reasonCode(),
                Timestamp.from(value.occurredAt()));
    }
}

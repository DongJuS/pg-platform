package com.shinhan.pg.common.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * [CMN-F01] 서비스 로컬 Outbox의 공통 JDBC 구현. 서비스는 이 클래스를 상속해
 * {@code adapter.out.persistence.outbox} 패키지에 두고, 업무 변경과 {@link #append}를
 * 같은 로컬 트랜잭션으로 묶는다.
 *
 * <p>claim은 행 잠금({@code FOR UPDATE SKIP LOCKED})과 지속 임대(claim_token)로 보호하며,
 * {@link #published}/{@link #retry}는 임대 토큰을 비교해 다른 워커의 임대를 완료하지 않는다.
 * PostgreSQL·MariaDB 공통 문법만 사용한다.
 *
 * <p>기대 테이블(서비스 마이그레이션 소유):
 * {@code outbox_events(id, event_id, destination, routing_key, payload, claim_token, claimed_at, published_at, attempts, created_at)}.
 */
public class JdbcOutboxStore implements OutboxStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate claimTx;

    public JdbcOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.claimTx = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
    }

    /** 업무 트랜잭션 안에서 호출하면 같은 커넥션에 참여하고, 단독 호출이면 자동 커밋된다. */
    @Override
    public void append(OutboxRecord record) {
        jdbc.update(
                "insert into outbox_events (id, event_id, destination, routing_key, payload)"
                        + " values (?, ?, ?, ?, ?)",
                record.id().toString(),
                record.eventId().toString(),
                record.destination(),
                record.key(),
                record.payload());
    }

    /** 임대 만료 전이거나 미발행인 행을 잠그고 임대를 기록해 돌려준다. 행 잠금은 이 트랜잭션 안에서만 유지된다. */
    @Override
    public List<OutboxRecord> claim(int limit, UUID claimToken, Duration lease) {
        return claimTx.execute(
                status -> {
                    var staleBefore = Timestamp.from(Instant.now().minus(lease));
                    var ids =
                            jdbc.query(
                                    "select id from outbox_events"
                                            + " where published_at is null"
                                            + " and (claimed_at is null or claimed_at < ?)"
                                            + " order by created_at, id limit ?"
                                            + " for update skip locked",
                                    (row, index) -> row.getString("id"),
                                    staleBefore,
                                    limit);
                    var records = new ArrayList<OutboxRecord>();
                    var claimedAt = Timestamp.from(Instant.now());
                    for (var id : ids) {
                        var updated =
                                jdbc.update(
                                        "update outbox_events set claim_token = ?, claimed_at = ?"
                                                + " where id = ? and published_at is null",
                                        claimToken.toString(),
                                        claimedAt,
                                        id);
                        if (updated == 1) {
                            records.add(load(id));
                        }
                    }
                    return records;
                });
    }

    /** 임대 토큰이 일치하는 워커만 발행 완료로 표시한다. */
    @Override
    public void published(UUID id, UUID claimToken) {
        jdbc.update(
                "update outbox_events set published_at = now()"
                        + " where id = ? and claim_token = ? and published_at is null",
                id.toString(),
                claimToken.toString());
    }

    /** 임대를 해제하고 재시도 횟수를 누적한다. 다음 claim에서 다시 선택된다. */
    @Override
    public void retry(UUID id, UUID claimToken) {
        jdbc.update(
                "update outbox_events set claim_token = null, claimed_at = null,"
                        + " attempts = attempts + 1"
                        + " where id = ? and claim_token = ? and published_at is null",
                id.toString(),
                claimToken.toString());
    }

    private OutboxRecord load(String id) {
        return jdbc.queryForObject(
                "select id, event_id, destination, routing_key, payload"
                        + " from outbox_events where id = ?",
                (row, index) ->
                        new OutboxRecord(
                                UUID.fromString(row.getString("id")),
                                UUID.fromString(row.getString("event_id")),
                                row.getString("destination"),
                                row.getString("routing_key"),
                                row.getBytes("payload")),
                id);
    }
}

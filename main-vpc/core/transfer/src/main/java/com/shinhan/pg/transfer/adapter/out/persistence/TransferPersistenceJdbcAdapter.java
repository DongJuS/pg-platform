package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.transfer.application.port.out.TransferQueryPort;
import com.shinhan.pg.transfer.application.port.out.TransferRepository;
import com.shinhan.pg.transfer.application.readmodel.TransferHistoryView;
import com.shinhan.pg.transfer.application.readmodel.TransferSummary;
import com.shinhan.pg.transfer.application.readmodel.UnknownTransferView;
import com.shinhan.pg.transfer.domain.model.Transfer;
import com.shinhan.pg.transfer.domain.policy.TransferStatePolicy;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F15,MVP1-F16,MVP1-F17,MVP1-F20] Transfer 영속성 구현 위치.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 *
 * <p>WO-A6 실구현: JdbcTemplate 기반 읽기 어댑터(JPA 미도입 — build에 JPA 없음).
 * 기존 TransferPersistenceAdapter/TransferHistoryPersistenceAdapter 스켈레톤은 유지하고
 * 이 클래스가 세 읽기 포트를 구현한다. 조회는 상태를 바꾸지 않는다.
 * 금액은 통화 scale 강제 Money로 복원한다(RoundingMode.UNNECESSARY — common-money 계약).</p>
 */
public final class TransferPersistenceJdbcAdapter implements TransferRepository, TransferQueryPort {
    private static final String TRANSFER_COLUMNS =
            "id, actor_user_id, from_account_id, to_account_id, amount, currency,"
                    + " idempotency_key, request_hash, transaction_id, status";

    private final JdbcTemplate jdbc;

    public TransferPersistenceJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    private final RowMapper<Transfer> transferMapper =
            (row, index) ->
                    new Transfer(
                            UUID.fromString(row.getString("id")),
                            UUID.fromString(row.getString("actor_user_id")),
                            UUID.fromString(row.getString("from_account_id")),
                            UUID.fromString(row.getString("to_account_id")),
                            Money.of(
                                    row.getBigDecimal("amount"),
                                    Currency.getInstance(row.getString("currency"))),
                            row.getString("idempotency_key"),
                            row.getString("request_hash"),
                            UUID.fromString(row.getString("transaction_id")),
                            row.getString("status"));

    @Override
    public Optional<Transfer> findById(UUID id) {
        return jdbc.query(
                        "select " + TRANSFER_COLUMNS + " from transfers where id = ?",
                        transferMapper,
                        id.toString())
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(UUID actorUserId, String idempotencyKey) {
        return jdbc.query(
                        "select " + TRANSFER_COLUMNS
                                + " from transfers where actor_user_id = ? and idempotency_key = ?",
                        transferMapper,
                        actorUserId.toString(),
                        idempotencyKey)
                .stream()
                .findFirst();
    }

    /** upsert — 신규는 insert, 기존 id면 상태·갱신 필드만 반영한다. 멱등 unique 충돌은 호출자가 다룬다. */
    @Override
    public void save(Transfer value) {
        jdbc.update(
                "insert into transfers (id, actor_user_id, from_account_id, to_account_id, amount,"
                        + " currency, idempotency_key, request_hash, transaction_id, status)"
                        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                        + " on conflict (id) do update set status = excluded.status",
                value.id().toString(),
                value.actorUserId().toString(),
                value.fromAccountId().toString(),
                value.toAccountId().toString(),
                value.amount().amount(),
                value.amount().currency().getCurrencyCode(),
                value.idempotencyKey(),
                value.requestHash(),
                value.transactionId().toString(),
                value.status());
    }

    @Override
    public Optional<UUID> findTransferIdByTransactionId(UUID transactionId) {
        return jdbc.query(
                        "select id from transfers where transaction_id = ?",
                        (row, index) -> UUID.fromString(row.getString("id")),
                        transactionId.toString())
                .stream()
                .findFirst();
    }

    @Override
    public Optional<String> findStatusById(UUID transferId) {
        return jdbc.query(
                        "select status from transfers where id = ?",
                        (row, index) -> row.getString("status"),
                        transferId.toString())
                .stream()
                .findFirst();
    }

    @Override
    public List<TransferHistoryView> findHistories(UUID transferId) {
        return jdbc.query(
                "select previous_status, next_status, reason_code, occurred_at"
                        + " from transfer_histories where transfer_id = ?"
                        + " order by occurred_at, id",
                (row, index) ->
                        new TransferHistoryView(
                                row.getString("previous_status"),
                                row.getString("next_status"),
                                row.getString("reason_code"),
                                row.getTimestamp("occurred_at").toInstant()),
                transferId.toString());
    }

    @Override
    public List<TransferSummary> findSummaries(
            UUID actorUserId, Instant from, Instant to, String status, int offset, int limit) {
        var sql =
                new StringBuilder(
                        "select id, amount, currency, status, created_at from transfers"
                                + " where actor_user_id = ?");
        var args = new ArrayList<>();
        args.add(actorUserId.toString());
        if (from != null) {
            sql.append(" and created_at >= ?");
            args.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" and created_at <= ?");
            args.add(Timestamp.from(to));
        }
        if (status != null) {
            sql.append(" and status = ?");
            args.add(status);
        }
        sql.append(" order by created_at desc, id limit ? offset ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(
                sql.toString(),
                (row, index) ->
                        new TransferSummary(
                                UUID.fromString(row.getString("id")),
                                row.getBigDecimal("amount"),
                                row.getString("currency"),
                                row.getString("status"),
                                row.getTimestamp("created_at").toInstant()),
                args.toArray());
    }

    @Override
    public List<UnknownTransferView> findUnknown(int limit) {
        return jdbc.query(
                "select id, transaction_id, created_at from transfers"
                        + " where status = ? order by created_at, id limit ?",
                (row, index) ->
                        new UnknownTransferView(
                                UUID.fromString(row.getString("id")),
                                UUID.fromString(row.getString("transaction_id")),
                                row.getTimestamp("created_at").toInstant()),
                TransferStatePolicy.UNKNOWN,
                limit);
    }
}

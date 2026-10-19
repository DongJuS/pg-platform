package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.ledger.adapter.out.persistence.entity.JournalEntity;
import com.shinhan.pg.ledger.adapter.out.persistence.entity.PostingEntity;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.JournalEntityMapper;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.domain.model.Journal;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F13,MVP1-F14] Journal 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 JournalEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 *
 * <p>구현: JdbcTemplate(ORM 없음). 분개 insert는 LedgerAtomicWriteAdapter의 잠금 트랜잭션이
 * 담당하고, 이 어댑터는 조회(F14)·eventId 복구와 테스트 편의의 save를 제공한다.
 */
public final class JournalPersistenceAdapter implements JournalRepository {
    private static final RowMapper<PostingEntity> POSTING_ROW_MAPPER =
            (row, index) ->
                    new PostingEntity(
                            UUID.fromString(row.getString("id")),
                            UUID.fromString(row.getString("journal_id")),
                            UUID.fromString(row.getString("ledger_account_id")),
                            row.getBigDecimal("signed_amount"),
                            row.getString("currency"));

    private final JdbcTemplate jdbc;
    private final JournalEntityMapper mapper;

    public JournalPersistenceAdapter(JdbcTemplate jdbc, JournalEntityMapper mapper) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<Journal> findById(UUID id) {
        var rows =
                jdbc.query(
                        "select id, transaction_id, posted_at from journals where id = ?",
                        this::mapJournalRow,
                        id.toString());
        return Optional.ofNullable(DataAccessUtils.singleResult(rows));
    }

    @Override
    public Optional<Journal> findByTransactionId(UUID transactionId) {
        var rows =
                jdbc.query(
                        "select id, transaction_id, posted_at from journals"
                                + " where transaction_id = ?",
                        this::mapJournalRow,
                        transactionId.toString());
        return Optional.ofNullable(DataAccessUtils.singleResult(rows));
    }

    @Override
    public Optional<UUID> findPostedEventId(UUID transactionId) {
        // ledger.posted Outbox 행은 업무 변경과 같은 로컬 트랜잭션에 기록되며 relay 후에도
        // 삭제되지 않으므로(published_at만 갱신) 여기서 원래 eventId를 복구할 수 있다.
        var rows =
                jdbc.query(
                        "select event_id from outbox_events"
                                + " where destination = ? and routing_key = ?"
                                + " order by created_at, id limit 1",
                        (row, index) -> UUID.fromString(row.getString("event_id")),
                        EventTopics.LEDGER_POSTED,
                        transactionId.toString());
        return rows.stream().findFirst();
    }

    @Override
    public void save(Journal value) {
        var entity = mapper.toEntity(value);
        jdbc.update(
                "insert into journals (id, transaction_id, posted_at) values (?, ?, ?)",
                entity.id().toString(),
                entity.transactionId().toString(),
                Timestamp.from(entity.postedAt()));
        var postingMapper = new com.shinhan.pg.ledger.adapter.out.persistence.mapper.PostingEntityMapper();
        for (var posting : value.postings()) {
            var postingEntity = postingMapper.toEntity(posting);
            jdbc.update(
                    "insert into postings (id, journal_id, ledger_account_id, signed_amount, currency)"
                            + " values (?, ?, ?, ?, ?)",
                    postingEntity.id().toString(),
                    postingEntity.journalId().toString(),
                    postingEntity.ledgerAccountId().toString(),
                    postingEntity.signedAmount(),
                    postingEntity.signedAmountCurrency());
        }
    }

    private Journal mapJournalRow(java.sql.ResultSet row, int index) throws java.sql.SQLException {
        var entity =
                new JournalEntity(
                        UUID.fromString(row.getString("id")),
                        UUID.fromString(row.getString("transaction_id")),
                        List.of(),
                        row.getTimestamp("posted_at").toInstant());
        var postings =
                jdbc.query(
                        "select id, journal_id, ledger_account_id, signed_amount, currency"
                                + " from postings where journal_id = ? order by id",
                        POSTING_ROW_MAPPER,
                        entity.id().toString());
        return mapper.toDomain(
                new JournalEntity(
                        entity.id(), entity.transactionId(), postings, entity.postedAt()));
    }
}

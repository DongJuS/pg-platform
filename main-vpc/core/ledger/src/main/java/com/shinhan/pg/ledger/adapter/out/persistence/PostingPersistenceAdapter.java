package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.ledger.adapter.out.persistence.entity.PostingEntity;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.PostingEntityMapper;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.readmodel.JournalEntryView;
import com.shinhan.pg.ledger.domain.model.Posting;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F12,MVP1-F13] Posting 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 PostingEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 *
 * <p>구현: JdbcTemplate(ORM 없음). 분개 insert는 LedgerAtomicWriteAdapter의 잠금 트랜잭션이
 * 담당하고, 이 어댑터는 거래내역 조회(F12)와 테스트 편의의 save를 제공한다.
 */
public final class PostingPersistenceAdapter implements PostingRepository {
    private static final RowMapper<PostingEntity> ROW_MAPPER =
            (row, index) ->
                    new PostingEntity(
                            UUID.fromString(row.getString("id")),
                            UUID.fromString(row.getString("journal_id")),
                            UUID.fromString(row.getString("ledger_account_id")),
                            row.getBigDecimal("signed_amount"),
                            row.getString("currency"));

    private final JdbcTemplate jdbc;
    private final PostingEntityMapper mapper;

    public PostingPersistenceAdapter(JdbcTemplate jdbc, PostingEntityMapper mapper) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<Posting> findById(UUID id) {
        var rows =
                jdbc.query(
                        "select id, journal_id, ledger_account_id, signed_amount, currency"
                                + " from postings where id = ?",
                        ROW_MAPPER,
                        id.toString());
        return Optional.ofNullable(DataAccessUtils.singleResult(rows)).map(mapper::toDomain);
    }

    @Override
    public List<JournalEntryView> findEntriesByExternalAccountId(
            UUID externalAccountId,
            Instant from,
            Instant to,
            Instant cursorPostedAt,
            UUID cursorJournalId,
            int limit) {
        var sql =
                new StringBuilder(
                        "select j.id as journal_id, j.transaction_id, p.signed_amount, p.currency,"
                                + " j.posted_at"
                                + " from postings p"
                                + " join journals j on j.id = p.journal_id"
                                + " join ledger_accounts la on la.id = p.ledger_account_id"
                                + " where la.external_account_id = ?");
        var args = new ArrayList<Object>();
        args.add(externalAccountId.toString());
        if (from != null) {
            sql.append(" and j.posted_at >= ?");
            args.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" and j.posted_at <= ?");
            args.add(Timestamp.from(to));
        }
        if (cursorPostedAt != null && cursorJournalId != null) {
            // 키셋 페이지네이션: (posted_at, journal_id) 역순 정렬의 다음 구간.
            sql.append(" and (j.posted_at < ? or (j.posted_at = ? and p.journal_id < ?))");
            args.add(Timestamp.from(cursorPostedAt));
            args.add(Timestamp.from(cursorPostedAt));
            args.add(cursorJournalId.toString());
        }
        sql.append(" order by j.posted_at desc, p.journal_id desc limit ?");
        args.add(limit);
        return jdbc.query(
                sql.toString(),
                (row, index) ->
                        new JournalEntryView(
                                UUID.fromString(row.getString("journal_id")),
                                UUID.fromString(row.getString("transaction_id")),
                                row.getBigDecimal("signed_amount"),
                                row.getString("currency"),
                                row.getTimestamp("posted_at").toInstant()),
                args.toArray());
    }

    @Override
    public void save(Posting value) {
        var entity = mapper.toEntity(value);
        jdbc.update(
                "insert into postings (id, journal_id, ledger_account_id, signed_amount, currency)"
                        + " values (?, ?, ?, ?, ?)",
                entity.id().toString(),
                entity.journalId().toString(),
                entity.ledgerAccountId().toString(),
                entity.signedAmount(),
                entity.signedAmountCurrency());
    }
}

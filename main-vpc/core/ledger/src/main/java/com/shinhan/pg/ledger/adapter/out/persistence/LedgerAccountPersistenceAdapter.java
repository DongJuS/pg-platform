package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.ledger.adapter.out.persistence.entity.LedgerAccountEntity;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.LedgerAccountEntityMapper;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F11,MVP1-F19] LedgerAccount 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 LedgerAccountEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 *
 * <p>구현: JdbcTemplate(ORM 없음). 읽기 전용 경로(F11)만 이 어댑터를 사용한다.
 * 잔액 갱신을 포함한 금융 쓰기는 LedgerAtomicWriteAdapter의 잠금 트랜잭션 안에서만 수행한다.
 */
public final class LedgerAccountPersistenceAdapter implements LedgerAccountRepository {
    private static final RowMapper<LedgerAccountEntity> ROW_MAPPER =
            (row, index) ->
                    new LedgerAccountEntity(
                            UUID.fromString(row.getString("id")),
                            UUID.fromString(row.getString("external_account_id")),
                            row.getBigDecimal("balance"),
                            row.getString("currency"),
                            row.getLong("version"));

    private final JdbcTemplate jdbc;
    private final LedgerAccountEntityMapper mapper;

    public LedgerAccountPersistenceAdapter(JdbcTemplate jdbc, LedgerAccountEntityMapper mapper) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<LedgerAccount> findById(UUID id) {
        var rows =
                jdbc.query(
                        "select id, external_account_id, balance, currency, version"
                                + " from ledger_accounts where id = ?",
                        ROW_MAPPER,
                        id.toString());
        return Optional.ofNullable(DataAccessUtils.singleResult(rows)).map(mapper::toDomain);
    }

    @Override
    public Optional<LedgerAccount> findByExternalAccountId(UUID externalAccountId) {
        var rows =
                jdbc.query(
                        "select id, external_account_id, balance, currency, version"
                                + " from ledger_accounts where external_account_id = ?",
                        ROW_MAPPER,
                        externalAccountId.toString());
        return Optional.ofNullable(DataAccessUtils.singleResult(rows)).map(mapper::toDomain);
    }

    @Override
    public void save(LedgerAccount value) {
        var entity = mapper.toEntity(value);
        jdbc.update(
                "insert into ledger_accounts (id, external_account_id, balance, currency, version)"
                        + " values (?, ?, ?, ?, ?)"
                        + " on duplicate key update balance = values(balance),"
                        + " currency = values(currency), version = values(version)",
                entity.id().toString(),
                entity.externalAccountId().toString(),
                entity.balance(),
                entity.balanceCurrency(),
                entity.version() == null ? 0L : entity.version());
    }
}

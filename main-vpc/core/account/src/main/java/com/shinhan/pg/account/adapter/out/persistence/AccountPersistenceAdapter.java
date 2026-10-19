package com.shinhan.pg.account.adapter.out.persistence;

import com.shinhan.pg.account.adapter.out.persistence.entity.AccountEntity;
import com.shinhan.pg.account.adapter.out.persistence.mapper.AccountEntityMapper;
import com.shinhan.pg.account.application.port.out.AccountRepository;
import com.shinhan.pg.account.domain.model.Account;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] Account 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 AccountEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 *
 * <p>구현 확정: JdbcTemplate(JPA 도입 금지 — build에 JPA 없음). 제약·unique는
 * AccountPersistenceIT가 실제 MariaDB에서 검증한다.
 */
public final class AccountPersistenceAdapter implements AccountRepository {
    private static final String COLUMNS =
            "id, owner_id, account_number, currency, alias, status, created_at";

    private final JdbcTemplate jdbc;
    private final AccountEntityMapper mapper = new AccountEntityMapper();

    public AccountPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jdbc.query(
                        "select " + COLUMNS + " from accounts where id = ?",
                        rowMapper(),
                        id.toString())
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public List<Account> findByOwnerId(UUID ownerId, int offset, int limit) {
        return jdbc.query(
                        "select " + COLUMNS + " from accounts where owner_id = ?"
                                + " order by created_at desc, id desc limit ? offset ?",
                        rowMapper(),
                        ownerId.toString(),
                        limit,
                        offset)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void save(Account value) {
        var entity = mapper.toEntity(value);
        // MariaDB upsert: PK(id) 충돌 시 메타데이터만 갱신한다. 잔액 컬럼은 존재하지 않는다(ledger 소유).
        jdbc.update(
                "insert into accounts (id, owner_id, account_number, currency, alias, status)"
                        + " values (?, ?, ?, ?, ?, ?)"
                        + " on duplicate key update"
                        + " owner_id = values(owner_id),"
                        + " account_number = values(account_number),"
                        + " currency = values(currency),"
                        + " alias = values(alias),"
                        + " status = values(status)",
                entity.id().toString(),
                entity.ownerId().toString(),
                entity.accountNumber(),
                entity.currency(),
                entity.alias(),
                entity.status());
    }

    private RowMapper<AccountEntity> rowMapper() {
        return (row, index) ->
                new AccountEntity(
                        UUID.fromString(row.getString("id")),
                        UUID.fromString(row.getString("owner_id")),
                        row.getString("account_number"),
                        row.getString("currency"),
                        row.getString("alias"),
                        row.getString("status"),
                        row.getTimestamp("created_at") == null
                                ? null
                                : row.getTimestamp("created_at").toInstant());
    }
}

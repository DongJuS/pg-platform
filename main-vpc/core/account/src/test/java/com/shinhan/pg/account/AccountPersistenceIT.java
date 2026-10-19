package com.shinhan.pg.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.account.adapter.out.persistence.AccountPersistenceAdapter;
import com.shinhan.pg.account.adapter.out.persistence.AccountWriteAdapter;
import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.domain.model.Account;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

/**
 * [MVP1-F07..F10] accounts 스키마와 JDBC 어댑터를 실제 MariaDB에서 검증한다(패턴: ledger FlywayMigrationIT).
 * 개설(ACTIVE)/조회/상태 전이/account_number unique 제약을 확인한다. 잔액 컬럼이 없음도 함께 확인한다.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class AccountPersistenceIT {
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> MARIADB =
            new GenericContainer<>(DockerImageName.parse("mariadb:11"))
                    .withEnv("MARIADB_ROOT_PASSWORD", "test")
                    .withEnv("MARIADB_DATABASE", "account_db")
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forListeningPort());

    private JdbcTemplate jdbc;
    private AccountWriteAdapter writeAdapter;
    private AccountPersistenceAdapter repository;

    @BeforeEach
    void migrateAndWire() {
        var url =
                "jdbc:mariadb://"
                        + MARIADB.getHost()
                        + ":"
                        + MARIADB.getMappedPort(3306)
                        + "/account_db";
        var source = new DriverManagerDataSource(url, "root", "test");
        Flyway.configure()
                .dataSource(source)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(source);
        // V1__baseline + V3__accounts 적용 확인 — 업무 테이블은 V3가 처음 만든다(V1은 인프라 baseline 주석뿐).
        var tables =
                jdbc.queryForList(
                        "select table_name from information_schema.tables"
                                + " where table_schema = 'account_db' and table_name = 'accounts'",
                        String.class);
        assertEquals(1, tables.size());
        jdbc.execute("delete from accounts");
        writeAdapter = new AccountWriteAdapter(jdbc);
        repository = new AccountPersistenceAdapter(jdbc);
    }

    @Test
    void accountsTableHasNoBalanceColumn() {
        var columns =
                jdbc.queryForList(
                        "select column_name from information_schema.columns"
                                + " where table_schema = 'account_db' and table_name = 'accounts'",
                        String.class);
        assertTrue(columns.contains("account_number"));
        // 잔액은 ledger가 소유한다 — balance/잔액 컬럼이 없어야 한다.
        assertTrue(columns.stream().noneMatch(c -> c.toLowerCase().contains("balance")));
    }

    @Test
    void openAccountPersistsActiveAccount() {
        var owner = UUID.randomUUID();
        var result =
                writeAdapter.create(
                        new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", "입출금"));
        assertEquals("ACTIVE", result.status());

        var found = repository.findById(result.accountId());
        assertTrue(found.isPresent());
        assertEquals(owner, found.get().ownerId());
        assertEquals(result.accountNumber(), found.get().accountNumber());
        assertEquals("KRW", found.get().currency());
        assertEquals("입출금", found.get().alias());
    }

    @Test
    void generatedAccountNumbersAreUniqueAcrossAccounts() {
        var owner = UUID.randomUUID();
        var first = writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", null));
        var second = writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", null));
        assertNotEquals(first.accountNumber(), second.accountNumber());
    }

    @Test
    void accountNumberUniqueConstraintIsEnforced() {
        var owner = UUID.randomUUID();
        writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", null));
        var existingNumber =
                jdbc.queryForObject("select account_number from accounts limit 1", String.class);
        // 같은 account_number의 직접 INSERT는 unique 제약으로 거부되어야 한다.
        assertThrows(
                DuplicateKeyException.class,
                () ->
                        jdbc.update(
                                "insert into accounts (id, owner_id, account_number, currency, alias, status)"
                                        + " values (?, ?, ?, 'KRW', null, 'ACTIVE')",
                                UUID.randomUUID().toString(),
                                owner.toString(),
                                existingNumber));
    }

    @Test
    void findByOwnerIdReturnsOnlyOwnedAccountsNewestFirst() {
        var owner = UUID.randomUUID();
        var other = UUID.randomUUID();
        writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", "a"));
        writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), other, "KRW", "b"));
        writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", "c"));

        var owned = repository.findByOwnerId(owner, 0, 10);
        assertEquals(2, owned.size());
        assertTrue(owned.stream().allMatch(a -> a.ownerId().equals(owner)));

        var paged = repository.findByOwnerId(owner, 1, 1);
        assertEquals(1, paged.size());
        assertEquals(owned.get(1).id(), paged.get(0).id());
    }

    @Test
    void saveUpdatesStatusInPlace() {
        var owner = UUID.randomUUID();
        var opened =
                writeAdapter.create(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", "별칭"));
        var account = repository.findById(opened.accountId()).orElseThrow();
        repository.save(
                new Account(
                        account.id(),
                        account.ownerId(),
                        account.accountNumber(),
                        account.currency(),
                        account.alias(),
                        "SUSPENDED"));
        var updated = repository.findById(opened.accountId()).orElseThrow();
        assertEquals("SUSPENDED", updated.status());
        // upsert가 행을 복제하지 않았는지 확인한다.
        assertEquals(1L, jdbc.queryForObject("select count(*) from accounts", Long.class));
    }
}

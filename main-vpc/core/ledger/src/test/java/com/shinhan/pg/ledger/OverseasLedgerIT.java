package com.shinhan.pg.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.common.swiftmessages.SwiftIds;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.ledger.adapter.out.persistence.LedgerAtomicWriteAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.outbox.LedgerOutboxStore;
import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.ReserveOverseasCommand;
import com.shinhan.pg.ledger.domain.error.LedgerException;
import com.shinhan.pg.ledger.domain.policy.BalanceProtectionPolicy;
import com.shinhan.pg.ledger.domain.policy.JournalBalancePolicy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class OverseasLedgerIT {
    @Container
    static final GenericContainer<?> MARIA =
            new GenericContainer<>("mariadb:11")
                    .withEnv("MARIADB_ROOT_PASSWORD", "test")
                    .withEnv("MARIADB_DATABASE", "ledger_db")
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forListeningPort());

    static DriverManagerDataSource ds;
    static JdbcTemplate jdbc;
    LedgerAtomicWriteAdapter writer;
    UUID source;
    ReserveOverseasCommand c;

    @BeforeAll
    static void migrate() {
        ds =
                new DriverManagerDataSource(
                        "jdbc:mariadb://"
                                + MARIA.getHost()
                                + ":"
                                + MARIA.getMappedPort(3306)
                                + "/ledger_db",
                        "root",
                        "test");
        Flyway.configure().dataSource(ds).load().migrate();
        jdbc = new JdbcTemplate(ds);
    }

    LedgerAtomicWriteAdapter writer() {
        return new LedgerAtomicWriteAdapter(
                jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(ds)),
                new LedgerOutboxStore(jdbc),
                new JournalBalancePolicy(),
                new BalanceProtectionPolicy());
    }

    @BeforeEach
    void initialize() {
        jdbc.execute("delete from overseas_reservations");
        jdbc.execute("delete from outbox_events");
        jdbc.execute("delete from postings");
        jdbc.execute("delete from journals");
        jdbc.execute("delete from ledger_accounts");
        writer = writer();
        source = UUID.randomUUID();
        jdbc.update(
                "insert into"
                    + " ledger_accounts(id,external_account_id,balance,currency,version)values(?,?"
                    + " ,100,'USD',0)",
                UUID.randomUUID().toString(),
                source.toString());
        c =
                new ReserveOverseasCommand(
                        UUID.randomUUID(),
                        UUID.randomUUID().toString(),
                        UUID.randomUUID(),
                        source,
                        new BigDecimal("30.00"),
                        "USD");
    }

    void outcome(String code) {
        writer.applyInstitutionResultWithPessimisticLocks(
                new ApplyInstitutionResultCommand(
                        UUID.randomUUID(), c.transferId(), c.uetr(), code));
    }

    BigDecimal balance(UUID id) {
        return jdbc.queryForObject(
                "select balance from ledger_accounts where external_account_id=?",
                BigDecimal.class,
                id.toString());
    }

    @Test
    void concurrentReservationsAndFinalResultsHaveOneFinancialEffect() throws Exception {
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(6)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 20; i++) futures.add(pool.submit(() -> writer.reserveOverseas(c)));
            for (var f : futures) f.get();
            futures.clear();
            for (int i = 0; i < 20; i++) futures.add(pool.submit(() -> outcome("ACSC")));
            for (var f : futures) f.get();
        }
        assertEquals(0, new BigDecimal("70").compareTo(balance(source)));
        assertEquals(0, balance(SwiftIds.id(c.transferId(), "holding-account")).signum());
        assertEquals(2, jdbc.queryForObject("select count(*) from journals", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from outbox_events where"
                                + " destination='ledger.overseas-applied'",
                        Integer.class));
        assertEquals(
                0,
                jdbc.queryForObject("select sum(signed_amount) from postings", BigDecimal.class)
                        .signum());
    }

    @Test
    void rejectionReturnsAllFundsOnce() {
        writer.reserveOverseas(c);
        outcome("RJCT");
        outcome("RJCT");
        assertEquals(0, new BigDecimal("100").compareTo(balance(source)));
        assertEquals(2, jdbc.queryForObject("select count(*) from journals", Integer.class));
    }

    @Test
    void conflictingFinalResultMarksReviewWithoutCompensatingAgain() {
        writer.reserveOverseas(c);
        outcome("ACSC");
        outcome("RJCT");
        outcome("RJCT");
        assertEquals(0, new BigDecimal("70").compareTo(balance(source)));
        assertEquals(2, jdbc.queryForObject("select count(*) from journals", Integer.class));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select review_required from overseas_reservations", Integer.class));
        assertEquals(
                2,
                jdbc.queryForObject(
                        "select count(*) from outbox_events where"
                                + " destination='ledger.overseas-applied'",
                        Integer.class));
    }

    @Test
    void uncertaintyDoesNotReleaseHold() {
        writer.reserveOverseas(c);
        writer = writer();
        assertEquals(
                0,
                new BigDecimal("30")
                        .compareTo(balance(SwiftIds.id(c.transferId(), "holding-account"))));
        assertEquals(1, jdbc.queryForObject("select count(*) from journals", Integer.class));
    }

    @Test
    void responseLossAndRestartReturnExistingJournal() {
        var first = writer.reserveOverseas(c);
        writer = writer();
        assertEquals(first.journalId(), writer.reserveOverseas(c).journalId());
        outcome("ACSC");
        writer = writer();
        outcome("ACSC");
        assertEquals(2, jdbc.queryForObject("select count(*) from journals", Integer.class));
    }

    @Test
    void changedReservationIdentityAndWrongResultCorrelationAreRejected() {
        writer.reserveOverseas(c);
        assertThrows(
                SwiftProblem.class,
                () ->
                        writer.reserveOverseas(
                                new ReserveOverseasCommand(
                                        c.transferId(),
                                        c.uetr(),
                                        c.actorUserId(),
                                        source,
                                        new BigDecimal("31.00"),
                                        "USD")));
        assertThrows(
                SwiftProblem.class,
                () ->
                        writer.applyInstitutionResultWithPessimisticLocks(
                                new ApplyInstitutionResultCommand(
                                        UUID.randomUUID(),
                                        c.transferId(),
                                        UUID.randomUUID().toString(),
                                        "ACSC")));
        assertEquals(1, jdbc.queryForObject("select count(*) from journals", Integer.class));
    }

    @Test
    void insufficientFundsRollBackReservationPostingAndOutbox() {
        var tooLarge =
                new ReserveOverseasCommand(
                        c.transferId(),
                        c.uetr(),
                        c.actorUserId(),
                        source,
                        new BigDecimal("101.00"),
                        "USD");
        assertThrows(LedgerException.class, () -> writer.reserveOverseas(tooLarge));
        assertEquals(0, jdbc.queryForObject("select count(*) from journals", Integer.class));
        assertEquals(
                0,
                jdbc.queryForObject("select count(*) from overseas_reservations", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from outbox_events", Integer.class));
        assertEquals(0, new BigDecimal("100").compareTo(balance(source)));
    }
}

package com.shinhan.pg.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.LedgerPostedEvent;
import com.shinhan.pg.ledger.adapter.out.persistence.JournalPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.LedgerAccountPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.LedgerAtomicWriteAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.PostingPersistenceAdapter;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.JournalEntityMapper;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.LedgerAccountEntityMapper;
import com.shinhan.pg.ledger.adapter.out.persistence.mapper.PostingEntityMapper;
import com.shinhan.pg.ledger.adapter.out.persistence.outbox.LedgerOutboxStore;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.command.PostingInstruction;
import com.shinhan.pg.ledger.application.result.PostJournalResult;
import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
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
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * [WO-A5 인수 기준] ADR 0001 6-A 잔액 불변식의 실제 MariaDB 검증. 같은 계좌에 대한 동시 출금(잔액 초과 혼합) 후에도 ① 잔액 = 초기 + 성공 분개
 * 합, ② 음수 잔액 없음, ③ journal unique(transactionId), ④ Outbox 행이 성공 분개와 1:1 임을 확인한다.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class LedgerPostingConcurrencyIT {
    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> MARIADB =
            new GenericContainer<>(DockerImageName.parse("mariadb:11"))
                    .withEnv("MARIADB_ROOT_PASSWORD", "test")
                    .withEnv("MARIADB_DATABASE", "ledger_db")
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forListeningPort());

    static DriverManagerDataSource source;
    static JdbcTemplate jdbc;

    LedgerAtomicWriteAdapter adapter;

    @BeforeAll
    static void migrate() {
        var url =
                "jdbc:mariadb://"
                        + MARIADB.getHost()
                        + ":"
                        + MARIADB.getMappedPort(3306)
                        + "/ledger_db";
        source = new DriverManagerDataSource(url, "root", "test");
        var result =
                Flyway.configure()
                        .dataSource(source)
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
        assertEquals(3, result.migrationsExecuted);
        jdbc = new JdbcTemplate(source);
    }

    @BeforeEach
    void setUp() {
        jdbc.execute("delete from outbox_events");
        jdbc.execute("delete from postings");
        jdbc.execute("delete from journals");
        jdbc.execute("delete from ledger_accounts");
        adapter =
                new LedgerAtomicWriteAdapter(
                        jdbc,
                        new TransactionTemplate(new DataSourceTransactionManager(source)),
                        new LedgerOutboxStore(jdbc),
                        new JournalBalancePolicy(),
                        new BalanceProtectionPolicy());
    }

    @Test
    void reportReadKeepsHistoricalBalanceAndGrossFlowAtOneCutoff() {
        UUID from = UUID.randomUUID(), to = UUID.randomUUID(), empty = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);
        var first =
                adapter.postWithPessimisticLocks(transferCommand(UUID.randomUUID(), from, to, 100));
        var cutoff = java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var second =
                adapter.postWithPessimisticLocks(transferCommand(UUID.randomUUID(), from, to, 200));
        // Fix historical fixture times, independent of clock/timing and with a later posting
        // present.
        jdbc.update(
                "update journals set posted_at=? where id=?",
                java.sql.Timestamp.from(cutoff.minusSeconds(10)),
                first.journalId().toString());
        jdbc.update(
                "update journals set posted_at=? where id=?",
                java.sql.Timestamp.from(cutoff.plusSeconds(10)),
                second.journalId().toString());
        var sourceRead =
                new com.shinhan.pg.ledger.adapter.out.persistence.ReportLedgerSnapshotAdapter(
                        source);
        var view =
                sourceRead.read(
                        UUID.randomUUID(),
                        List.of(from, to, empty),
                        cutoff.minusSeconds(20),
                        cutoff,
                        cutoff);
        assertEquals(3, view.accounts().size());
        assertEquals(0, view.accounts().get(0).balance().compareTo(BigDecimal.valueOf(900)));
        assertEquals(0, view.accounts().get(0).cashOut().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, view.accounts().get(1).balance().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, view.accounts().get(1).cashIn().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, view.accounts().get(2).balance().signum());
        assertTrue(!view.capturedAt().isBefore(view.asOf()));
    }

    @Test
    void reportCollectionDoesNotTearAcrossConcurrentAccountUpdates() throws Exception {
        UUID from = UUID.randomUUID(), to = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);
        var reader =
                new com.shinhan.pg.ledger.adapter.out.persistence.ReportLedgerSnapshotAdapter(
                        source);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var writer =
                    pool.submit(
                            () -> {
                                for (int i = 0; i < 20; i++)
                                    adapter.postWithPessimisticLocks(
                                            transferCommand(UUID.randomUUID(), from, to, 1));
                            });
            for (int i = 0; i < 10; i++) {
                var cutoff =
                        java.time.Instant.now()
                                .minusSeconds(1)
                                .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                var view =
                        reader.read(
                                UUID.randomUUID(),
                                List.of(from, to),
                                cutoff.minusSeconds(60),
                                cutoff,
                                cutoff);
                var sum =
                        view.accounts().stream()
                                .map(a -> a.balance())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertEquals(0, sum.compareTo(BigDecimal.valueOf(1000)));
            }
            writer.get(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 핵심 인수 기준: 잔액 1000인 계좌에 100씩 16건의 동시 출금(잔액 초과 혼합). 정확히 10건만 성공하고 6건은 INSUFFICIENT_FUNDS로 거절돼야
     * 한다.
     */
    @Test
    void concurrentDebitsPreserveBalanceInvariant() throws Exception {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);

        int attempts = 16;
        var pool = Executors.newFixedThreadPool(attempts);
        var ready = new CountDownLatch(attempts);
        var start = new CountDownLatch(1);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<Outcome>>();
        for (int i = 0; i < attempts; i++) {
            var command = transferCommand(UUID.randomUUID(), from, to, 100);
            futures.add(
                    pool.submit(
                            () -> {
                                ready.countDown();
                                start.await();
                                try {
                                    return Outcome.posted(
                                            adapter.postWithPessimisticLocks(command));
                                } catch (LedgerException rejected) {
                                    return Outcome.rejected(rejected.code());
                                }
                            }));
        }
        assertTrue(ready.await(30, TimeUnit.SECONDS));
        start.countDown();

        var outcomes = new java.util.ArrayList<Outcome>();
        for (var future : futures) {
            outcomes.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        long succeeded = outcomes.stream().filter(o -> o.posted != null).count();
        var rejected =
                outcomes.stream().filter(o -> o.errorCode != null).map(o -> o.errorCode).toList();
        // 전부 결과가 있고, 거절 사유는 잔액 부족뿐이며, 잔액 1000/100 → 정확히 10건 성공.
        assertEquals(attempts, succeeded + rejected.size());
        assertTrue(rejected.stream().allMatch(code -> code == LedgerErrorCode.INSUFFICIENT_FUNDS));
        assertEquals(10, succeeded);

        // ① 잔액 = 초기 + 성공 분개 합, ② 음수 잔액 없음.
        assertEquals(0, balanceOf(from).compareTo(BigDecimal.valueOf(1000 - 100 * succeeded)));
        assertEquals(0, balanceOf(to).compareTo(BigDecimal.valueOf(100 * succeeded)));
        assertTrue(balanceOf(from).signum() >= 0);

        // ③ 성공 분개만 journals/outbox에 남고 1:1이다.
        assertEquals(succeeded, count("journals"));
        assertEquals(succeeded, count("outbox_events"));
        assertEquals(2 * succeeded, count("postings"));

        // 저장된 분개는 통화별 합계 0이며, 계좌 posting 합과 잔액 변화가 일치한다.
        var unbalanced =
                jdbc.queryForObject(
                        "select count(*) from (select journal_id, currency, sum(signed_amount) s"
                                + " from postings group by journal_id, currency having s <> 0) t",
                        Long.class);
        assertEquals(0L, unbalanced);
        var postedSumFrom = postingSumOf(from);
        assertEquals(0, postedSumFrom.compareTo(BigDecimal.valueOf(-100 * succeeded)));
    }

    /** 멱등: 같은 transactionId의 순차·동시 재요청은 같은 journal을 반환하고 한 번만 반영한다. */
    @Test
    void duplicateTransactionIdReturnsExistingJournalExactlyOnce() throws Exception {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);
        var transactionId = UUID.randomUUID();

        var first = adapter.postWithPessimisticLocks(transferCommand(transactionId, from, to, 100));
        var second =
                adapter.postWithPessimisticLocks(transferCommand(transactionId, from, to, 100));
        assertEquals(first.journalId(), second.journalId());
        assertEquals(first.eventId(), second.eventId());
        assertEquals(1L, count("journals"));
        assertEquals(1L, count("outbox_events"));
        assertEquals(0, balanceOf(from).compareTo(BigDecimal.valueOf(900)));

        // 동시 중복: unique(transaction_id) 경합은 재시도 후 기존 journal로 수렴한다.
        var concurrentTxId = UUID.randomUUID();
        var pool = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<PostJournalResult>>();
        for (int i = 0; i < 4; i++) {
            var command = transferCommand(concurrentTxId, from, to, 100);
            futures.add(
                    pool.submit(
                            () -> {
                                start.await();
                                return adapter.postWithPessimisticLocks(command);
                            }));
        }
        start.countDown();
        var results = new java.util.ArrayList<PostJournalResult>();
        for (var future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        var distinctJournals =
                results.stream().map(PostJournalResult::journalId).distinct().count();
        assertEquals(1, distinctJournals);
        assertEquals(2L, count("journals"));
        assertEquals(2L, count("outbox_events"));
        // 잔액은 두 분개(각 100)만 반영된다.
        assertEquals(0, balanceOf(from).compareTo(BigDecimal.valueOf(800)));
        assertEquals(0, balanceOf(to).compareTo(BigDecimal.valueOf(200)));
    }

    /** 합계≠0 분개는 거절되고 어떤 행도 남지 않는다(단일 트랜잭션 롤백). */
    @Test
    void unbalancedJournalIsRejectedAndRolledBack() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);
        var command =
                new PostJournalCommand(
                        UUID.randomUUID(),
                        "it",
                        UUID.randomUUID(),
                        List.of(
                                new PostingInstruction(from, new BigDecimal("-100"), "KRW"),
                                new PostingInstruction(to, new BigDecimal("99"), "KRW")));
        var error =
                assertThrows(
                        LedgerException.class, () -> adapter.postWithPessimisticLocks(command));
        assertEquals(LedgerErrorCode.UNBALANCED_JOURNAL, error.code());
        assertEquals(0L, count("journals"));
        assertEquals(0L, count("postings"));
        assertEquals(0L, count("outbox_events"));
        assertEquals(0, balanceOf(from).compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, balanceOf(to).compareTo(BigDecimal.ZERO));
    }

    /** 잔액 부족 출금은 거절되고 잔액·journal·outbox 모두 롤백된다. */
    @Test
    void insufficientFundsIsRejectedAndRolledBack() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        seedAccount(from, 50);
        seedAccount(to, 0);
        var error =
                assertThrows(
                        LedgerException.class,
                        () ->
                                adapter.postWithPessimisticLocks(
                                        transferCommand(UUID.randomUUID(), from, to, 100)));
        assertEquals(LedgerErrorCode.INSUFFICIENT_FUNDS, error.code());
        assertEquals(0L, count("journals"));
        assertEquals(0L, count("outbox_events"));
        assertEquals(0, balanceOf(from).compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, balanceOf(to).compareTo(BigDecimal.ZERO));
    }

    /** 지시서 §5-② 결정안(lazy upsert): 분개 시점에 없는 원장 계정은 0 잔액으로 생성된다. */
    @Test
    void firstPostingLazilyProvisionsLedgerAccount() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID(); // 미개설 원장 계정
        seedAccount(from, 1000);
        var result =
                adapter.postWithPessimisticLocks(transferCommand(UUID.randomUUID(), from, to, 100));
        assertNotNull(result.journalId());
        assertEquals(0, balanceOf(to).compareTo(BigDecimal.valueOf(100)));
        var currency =
                jdbc.queryForObject(
                        "select currency from ledger_accounts where external_account_id = ?",
                        String.class,
                        to.toString());
        assertEquals("KRW", currency);
    }

    /** ledger.posted Outbox payload는 WO-A3 계약(eventId/transactionId/journalId/occurredAt)과 일치한다. */
    @Test
    void ledgerPostedOutboxPayloadMatchesEventContract() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);
        var command = transferCommand(UUID.randomUUID(), from, to, 100);
        var result = adapter.postWithPessimisticLocks(command);

        var row = jdbc.queryForMap("select destination, routing_key, payload from outbox_events");
        assertEquals(EventTopics.LEDGER_POSTED, row.get("destination"));
        assertEquals(command.transactionId().toString(), row.get("routing_key"));
        var event = EventCodec.fromBytes((byte[]) row.get("payload"), LedgerPostedEvent.class);
        assertEquals(result.eventId(), event.eventId());
        assertEquals(command.transactionId(), event.transactionId());
        assertEquals(result.journalId(), event.journalId());
        assertNotNull(event.occurredAt());
    }

    /** F11/F12/F14 읽기 경로: 반영된 분개가 잔액·거래내역·반영 결과 조회에 노출된다. */
    @Test
    void readPathsExposePostedJournal() {
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        seedAccount(from, 1000);
        seedAccount(to, 0);
        var command = transferCommand(UUID.randomUUID(), from, to, 100);
        var result = adapter.postWithPessimisticLocks(command);

        var accounts = new LedgerAccountPersistenceAdapter(jdbc, new LedgerAccountEntityMapper());
        var journals = new JournalPersistenceAdapter(jdbc, new JournalEntityMapper());
        var postings = new PostingPersistenceAdapter(jdbc, new PostingEntityMapper());

        // F11
        var account = accounts.findByExternalAccountId(from).orElseThrow();
        assertEquals(0, account.balance().amount().compareTo(BigDecimal.valueOf(900)));
        assertEquals("KRW", account.balance().currency().getCurrencyCode());

        // F14 (+eventId 복구)
        var journal = journals.findByTransactionId(command.transactionId()).orElseThrow();
        assertEquals(result.journalId(), journal.id());
        assertEquals(2, journal.postings().size());
        assertEquals(
                result.eventId(),
                journals.findPostedEventId(command.transactionId()).orElseThrow());

        // F12 (시간 역순, 계좌별)
        var entries = postings.findEntriesByExternalAccountId(from, null, null, null, null, 10);
        assertEquals(1, entries.size());
        assertEquals(result.journalId(), entries.get(0).journalId());
        assertEquals(command.transactionId(), entries.get(0).transactionId());
        assertEquals(0, entries.get(0).signedAmount().compareTo(BigDecimal.valueOf(-100)));
    }

    // ---- helpers ----

    private record Outcome(PostJournalResult posted, LedgerErrorCode errorCode) {
        static Outcome posted(PostJournalResult result) {
            return new Outcome(result, null);
        }

        static Outcome rejected(LedgerErrorCode code) {
            return new Outcome(null, code);
        }
    }

    private static PostJournalCommand transferCommand(
            UUID transactionId, UUID from, UUID to, long amount) {
        return new PostJournalCommand(
                UUID.randomUUID(),
                "it",
                transactionId,
                List.of(
                        new PostingInstruction(from, BigDecimal.valueOf(-amount), "KRW"),
                        new PostingInstruction(to, BigDecimal.valueOf(amount), "KRW")));
    }

    private static void seedAccount(UUID externalAccountId, long balanceWon) {
        jdbc.update(
                "insert into ledger_accounts (id, external_account_id, balance, currency, version)"
                        + " values (?, ?, ?, 'KRW', 0)",
                UUID.randomUUID().toString(),
                externalAccountId.toString(),
                BigDecimal.valueOf(balanceWon));
    }

    private static BigDecimal balanceOf(UUID externalAccountId) {
        return jdbc.queryForObject(
                "select balance from ledger_accounts where external_account_id = ?",
                BigDecimal.class,
                externalAccountId.toString());
    }

    private static BigDecimal postingSumOf(UUID externalAccountId) {
        return jdbc.queryForObject(
                "select coalesce(sum(p.signed_amount), 0) from postings p"
                        + " join ledger_accounts la on la.id = p.ledger_account_id"
                        + " where la.external_account_id = ?",
                BigDecimal.class,
                externalAccountId.toString());
    }

    private static long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }
}

package com.shinhan.pg.ledger.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.command.GetBalanceCommand;
import com.shinhan.pg.ledger.application.command.ListJournalEntriesCommand;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.command.PostingInstruction;
import com.shinhan.pg.ledger.application.command.QueryJournalCommand;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.readmodel.JournalEntryView;
import com.shinhan.pg.ledger.application.result.PostJournalResult;
import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
import com.shinhan.pg.ledger.domain.error.LedgerException;
import com.shinhan.pg.ledger.domain.model.Journal;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;
import com.shinhan.pg.ledger.domain.model.Posting;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** F11~F14 유스케이스의 입력 검증·조회 매핑을 in-memory 페이크로 검증한다(DB 동작은 IT 담당). */
class LedgerApplicationServiceTest {

    // ---- F13 입력 검증 ----

    @Test
    void postJournalRejectsMissingTransactionId() {
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings());
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.postJournal(
                                        new PostJournalCommand(
                                                UUID.randomUUID(), "it", null, List.of())));
        assertEquals(ErrorType.INVALID, error.errorCode().type());
    }

    @Test
    void postJournalRejectsEmptyPostings() {
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings());
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.postJournal(
                                        new PostJournalCommand(
                                                UUID.randomUUID(),
                                                "it",
                                                UUID.randomUUID(),
                                                List.of())));
        assertEquals(ErrorType.INVALID, error.errorCode().type());
    }

    @Test
    void postJournalRejectsUnknownCurrency() {
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings());
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.postJournal(
                                        new PostJournalCommand(
                                                UUID.randomUUID(),
                                                "it",
                                                UUID.randomUUID(),
                                                List.of(
                                                        new PostingInstruction(
                                                                UUID.randomUUID(),
                                                                BigDecimal.TEN,
                                                                "ZZZ")))));
        assertEquals(ErrorType.INVALID, error.errorCode().type());
    }

    @Test
    void postJournalDelegatesToAtomicWritePort() {
        var atomicWrite = new RecordingAtomicWrite();
        var service = service(atomicWrite, emptyAccounts(), emptyJournals(), emptyPostings());
        var command =
                new PostJournalCommand(
                        UUID.randomUUID(),
                        "transfer",
                        UUID.randomUUID(),
                        List.of(
                                new PostingInstruction(
                                        UUID.randomUUID(), new BigDecimal("-100"), "KRW"),
                                new PostingInstruction(
                                        UUID.randomUUID(), new BigDecimal("100"), "KRW")));
        var result = service.postJournal(command);
        assertEquals(command.transactionId(), result.transactionId());
        assertEquals(1, atomicWrite.calls);
    }

    // ---- F14 ----

    @Test
    void queryJournalReturns404SignalWhenMissing() {
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings());
        var error =
                assertThrows(
                        LedgerException.class,
                        () ->
                                service.queryJournal(
                                        new QueryJournalCommand(
                                                UUID.randomUUID(), "it", UUID.randomUUID())));
        assertEquals(LedgerErrorCode.TRANSACTION_UNKNOWN, error.code());
    }

    @Test
    void queryJournalReturnsStoredJournalWithEventId() {
        var journals = emptyJournals();
        var transactionId = UUID.randomUUID();
        var journalId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        journals.journals.put(
                transactionId,
                new Journal(journalId, transactionId, List.of(), Instant.now()));
        journals.eventIds.put(transactionId, eventId);
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), journals, emptyPostings());
        var result =
                service.queryJournal(new QueryJournalCommand(UUID.randomUUID(), "it", transactionId));
        assertEquals(transactionId, result.transactionId());
        assertEquals(journalId, result.journalId());
        assertEquals(eventId, result.eventId());
        assertEquals("POSTED", result.status());
    }

    // ---- F11 ----

    @Test
    void getBalanceReturns404SignalWhenAccountMissing() {
        var service = new LedgerApplicationService(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings(), id -> {
            throw new ApplicationException(new com.shinhan.pg.common.commonerror.ErrorCode() {
                public String code() { return "ACCOUNT-NOT-FOUND"; }
                public String message() { return "Account not found"; }
                public ErrorType type() { return ErrorType.NOT_FOUND; }
            });
        });
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.getBalance(
                                        new GetBalanceCommand(
                                                UUID.randomUUID(),
                                                new UUID(0, 1),
                                                UUID.randomUUID())));
        assertEquals(ErrorType.NOT_FOUND, error.errorCode().type());
    }

    @Test
    void getBalanceReturnsStoredBalance() {
        var accounts = emptyAccounts();
        var externalId = UUID.randomUUID();
        accounts.accounts.put(
                externalId,
                new LedgerAccount(UUID.randomUUID(), externalId, Money.of(1000), 3L));
        var service = service(new RecordingAtomicWrite(), accounts, emptyJournals(), emptyPostings());
        var result =
                service.getBalance(
                        new GetBalanceCommand(UUID.randomUUID(), new UUID(0, 1), externalId));
        assertEquals(externalId, result.accountId());
        assertEquals(0, result.balance().compareTo(new BigDecimal("1000")));
        assertEquals("KRW", result.currency());
        assertNotNull(result.asOf());
    }

    // ---- F12 ----

    @Test
    void listJournalEntriesRejectsInvalidCursor() {
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings());
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.listJournalEntries(
                                        new ListJournalEntriesCommand(
                                                UUID.randomUUID(),
                                                new UUID(0, 1),
                                                UUID.randomUUID(),
                                                null,
                                                null,
                                                "not-a-cursor",
                                                null)));
        assertEquals(ErrorType.INVALID, error.errorCode().type());
    }

    @Test
    void listJournalEntriesRejectsInvertedRange() {
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), emptyPostings());
        var now = Instant.now();
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.listJournalEntries(
                                        new ListJournalEntriesCommand(
                                                UUID.randomUUID(),
                                                new UUID(0, 1),
                                                UUID.randomUUID(),
                                                now,
                                                now.minusSeconds(60),
                                                null,
                                                null)));
        assertEquals(ErrorType.INVALID, error.errorCode().type());
    }

    @Test
    void listJournalEntriesPaginatesWithNextCursor() {
        var postings = emptyPostings();
        var accountId = UUID.randomUUID();
        // size=2 요청에 3건(size+1)이 오면 nextCursor가 생긴다.
        for (int i = 0; i < 3; i++) {
            postings.rows.add(
                    new JournalEntryView(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            BigDecimal.TEN,
                            "KRW",
                            Instant.now().minusSeconds(i)));
        }
        var service = service(new RecordingAtomicWrite(), emptyAccounts(), emptyJournals(), postings);
        var first =
                service.listJournalEntries(
                        new ListJournalEntriesCommand(
                                UUID.randomUUID(), new UUID(0, 1), accountId, null, null, null, 2));
        assertEquals(2, first.entries().size());
        assertNotNull(first.nextCursor());

        postings.rows = new ArrayList<>(postings.rows.subList(2, 3));
        var second =
                service.listJournalEntries(
                        new ListJournalEntriesCommand(
                                UUID.randomUUID(),
                                new UUID(0, 1),
                                accountId,
                                null,
                                null,
                                first.nextCursor(),
                                2));
        assertEquals(1, second.entries().size());
        assertNull(second.nextCursor());
    }

    // ---- fakes ----

    private static LedgerApplicationService service(
            LedgerAtomicWritePort atomicWrite,
            LedgerAccountRepository accounts,
            JournalRepository journals,
            PostingRepository postings) {
        return new LedgerApplicationService(atomicWrite, accounts, journals, postings, id -> new com.shinhan.pg.ledger.application.readmodel.AccountAccessSnapshot(id, new UUID(0, 1), "KRW", "ACTIVE"));
    }

    private static final class RecordingAtomicWrite implements LedgerAtomicWritePort {
        int calls;

        @Override
        public PostJournalResult postWithPessimisticLocks(PostJournalCommand command) {
            calls++;
            return new PostJournalResult(
                    command.transactionId(), UUID.randomUUID(), UUID.randomUUID());
        }

        @Override
        public void applyInstitutionResultWithPessimisticLocks(
                ApplyInstitutionResultCommand command) {}
    }

    private static FakeAccounts emptyAccounts() {
        return new FakeAccounts();
    }

    private static final class FakeAccounts implements LedgerAccountRepository {
        final Map<UUID, LedgerAccount> accounts = new HashMap<>();

        @Override
        public Optional<LedgerAccount> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<LedgerAccount> findByExternalAccountId(UUID externalAccountId) {
            return Optional.ofNullable(accounts.get(externalAccountId));
        }

        @Override
        public void save(LedgerAccount value) {}
    }

    private static FakeJournals emptyJournals() {
        return new FakeJournals();
    }

    private static final class FakeJournals implements JournalRepository {
        final Map<UUID, Journal> journals = new HashMap<>();
        final Map<UUID, UUID> eventIds = new HashMap<>();

        @Override
        public Optional<Journal> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public void save(Journal value) {}

        @Override
        public Optional<Journal> findByTransactionId(UUID transactionId) {
            return Optional.ofNullable(journals.get(transactionId));
        }

        @Override
        public Optional<UUID> findPostedEventId(UUID transactionId) {
            return Optional.ofNullable(eventIds.get(transactionId));
        }
    }

    private static FakePostings emptyPostings() {
        return new FakePostings();
    }

    private static final class FakePostings implements PostingRepository {
        List<JournalEntryView> rows = new ArrayList<>();

        @Override
        public Optional<Posting> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public List<JournalEntryView> findEntriesByExternalAccountId(
                UUID externalAccountId,
                Instant from,
                Instant to,
                Instant cursorPostedAt,
                UUID cursorJournalId,
                int limit) {
            return rows;
        }

        @Override
        public void save(Posting value) {}
    }
}

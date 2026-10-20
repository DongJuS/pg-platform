package com.shinhan.pg.account.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.account.application.command.ChangeAccountStatusCommand;
import com.shinhan.pg.account.application.command.GetAccountCommand;
import com.shinhan.pg.account.application.command.ListAccountsCommand;
import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.port.out.AccountRepository;
import com.shinhan.pg.account.application.port.out.AccountWritePort;
import com.shinhan.pg.account.application.port.out.LedgerAccountPort;
import com.shinhan.pg.account.application.result.OpenAccountResult;
import com.shinhan.pg.account.domain.error.AccountErrorCode;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** [MVP1-F07..F10] 애플리케이션 서비스 계약: 인증 주체 검사, 소유권 검사, 페이지네이션, 상태 전이. */
class AccountApplicationServiceTest {
    private final FakeLedgerAccountPort ledgerPort = new FakeLedgerAccountPort();
    private final FakeAccountWritePort writePort = new FakeAccountWritePort();
    private final FakeAccountRepository repository = new FakeAccountRepository();
    private final AccountApplicationService service =
            new AccountApplicationService(ledgerPort, writePort, repository);

    // --- F07 개설 ---

    @Test
    void openAccountRejectsMissingActor() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.openAccount(
                                        new OpenAccountCommand(
                                                UUID.randomUUID(), null, "KRW", "별칭")));
        assertEquals(AccountErrorCode.UNAUTHENTICATED, error.errorCode());
    }

    @Test
    void openAccountRejectsNonKrwCurrency() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.openAccount(
                                        new OpenAccountCommand(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                "USD",
                                                null)));
        assertEquals(AccountErrorCode.INVALID_REQUEST, error.errorCode());
    }

    @Test
    void confirmedMvp5DevelopmentScopeCanOpenUsdButRejectsOtherCurrencies() {
        var enabled = new AccountApplicationService(ledgerPort, writePort, repository, true);
        var command = new OpenAccountCommand(UUID.randomUUID(), UUID.randomUUID(), "USD", "SWIFT");
        assertEquals("USD", enabled.openAccount(command).currency());
        assertThrows(
                ApplicationException.class,
                () ->
                        enabled.openAccount(
                                new OpenAccountCommand(
                                        UUID.randomUUID(), command.actorUserId(), "EUR", null)));
    }

    @Test
    void openAccountCreatesActiveAccountWithoutLedgerCall() {
        var owner = UUID.randomUUID();
        var result =
                service.openAccount(new OpenAccountCommand(UUID.randomUUID(), owner, "KRW", "입출금"));
        assertEquals("ACTIVE", result.status());
        assertEquals(owner, writePort.lastCommand.actorUserId());
        // 원장 provision 경로는 미결(§5-②) — 호출되지 않아야 한다.
        assertEquals(0, ledgerPort.provisionCalls);
    }

    // --- F08 목록 ---

    @Test
    void listAccountsRejectsMissingActor() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.listAccounts(
                                        new ListAccountsCommand(UUID.randomUUID(), null, 0, 10)));
        assertEquals(AccountErrorCode.UNAUTHENTICATED, error.errorCode());
    }

    @Test
    void listAccountsPaginatesWithNextPage() {
        var owner = UUID.randomUUID();
        for (int i = 0; i < 3; i++) repository.store.add(account(owner));

        var first = service.listAccounts(new ListAccountsCommand(UUID.randomUUID(), owner, 0, 2));
        assertEquals(2, first.accounts().size());
        assertEquals(1, first.nextPage());

        var second = service.listAccounts(new ListAccountsCommand(UUID.randomUUID(), owner, 1, 2));
        assertEquals(1, second.accounts().size());
        assertNull(second.nextPage());
    }

    @Test
    void listAccountsRejectsInvalidPaging() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.listAccounts(
                                        new ListAccountsCommand(
                                                UUID.randomUUID(), UUID.randomUUID(), 0, 0)));
        assertEquals(AccountErrorCode.INVALID_REQUEST, error.errorCode());
    }

    // --- F09 상세 ---

    @Test
    void getAccountReturns404WhenMissing() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.getAccount(
                                        new GetAccountCommand(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                UUID.randomUUID())));
        assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, error.errorCode());
    }

    @Test
    void getAccountReturns403ForOtherOwner() {
        var owner = UUID.randomUUID();
        var account = account(owner);
        repository.store.add(account);
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.getAccount(
                                        new GetAccountCommand(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                account.id())));
        assertEquals(AccountErrorCode.ACCOUNT_FORBIDDEN, error.errorCode());
    }

    @Test
    void getAccountReturnsDetailForOwner() {
        var owner = UUID.randomUUID();
        var account = account(owner);
        repository.store.add(account);
        var result =
                service.getAccount(new GetAccountCommand(UUID.randomUUID(), owner, account.id()));
        assertEquals(account.accountNumber(), result.accountNumber());
        assertEquals(owner, result.ownerId());
    }

    // --- F10 상태 변경 ---

    @Test
    void changeAccountStatusAppliesAllowedTransition() {
        var account = account(UUID.randomUUID());
        repository.store.add(account);
        var result =
                service.changeAccountStatus(
                        new ChangeAccountStatusCommand(
                                UUID.randomUUID(), null, "SUSPENDED", "사유", account.id()));
        assertEquals("SUSPENDED", result.status());
        assertEquals("SUSPENDED", repository.store.get(0).status());
    }

    @Test
    void changeAccountStatusRejectsInvalidTransition() {
        var account = account(UUID.randomUUID());
        repository.store.add(account);
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.changeAccountStatus(
                                        new ChangeAccountStatusCommand(
                                                UUID.randomUUID(),
                                                null,
                                                "CLOSED",
                                                "사유",
                                                account.id())));
        assertEquals(AccountErrorCode.INVALID_STATUS_TRANSITION, error.errorCode());
    }

    @Test
    void changeAccountStatusReturns404WhenMissing() {
        var error =
                assertThrows(
                        ApplicationException.class,
                        () ->
                                service.changeAccountStatus(
                                        new ChangeAccountStatusCommand(
                                                UUID.randomUUID(),
                                                null,
                                                "SUSPENDED",
                                                "사유",
                                                UUID.randomUUID())));
        assertEquals(AccountErrorCode.ACCOUNT_NOT_FOUND, error.errorCode());
    }

    private static Account account(UUID owner) {
        return new Account(UUID.randomUUID(), owner, "1234567890123456", "KRW", "별칭", "ACTIVE");
    }

    // --- fakes ---

    private static final class FakeLedgerAccountPort implements LedgerAccountPort {
        int provisionCalls;

        @Override
        public UUID provision(OpenAccountCommand command) {
            provisionCalls++;
            return UUID.randomUUID();
        }
    }

    private static final class FakeAccountWritePort implements AccountWritePort {
        OpenAccountCommand lastCommand;

        @Override
        public OpenAccountResult create(OpenAccountCommand command) {
            lastCommand = command;
            return new OpenAccountResult(
                    UUID.randomUUID(), "1234567890123456", command.currency(), "ACTIVE");
        }
    }

    private static final class FakeAccountRepository implements AccountRepository {
        final List<Account> store = new ArrayList<>();

        @Override
        public Optional<Account> findById(UUID id) {
            return store.stream().filter(a -> a.id().equals(id)).findFirst();
        }

        @Override
        public List<Account> findByOwnerId(UUID ownerId, int offset, int limit) {
            var owned = store.stream().filter(a -> a.ownerId().equals(ownerId)).toList();
            return owned.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public void save(Account value) {
            store.removeIf(a -> a.id().equals(value.id()));
            store.add(value);
        }
    }
}

package com.shinhan.pg.ledger;

import com.shinhan.pg.ledger.application.command.GetBalanceCommand;
import com.shinhan.pg.ledger.application.port.out.JournalRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.application.port.out.LedgerAtomicWritePort;
import com.shinhan.pg.ledger.application.port.out.PostingRepository;
import com.shinhan.pg.ledger.application.readmodel.AccountAccessSnapshot;
import com.shinhan.pg.ledger.application.service.LedgerApplicationService;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorType;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LedgerAccessTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();
    private LedgerApplicationService service(LedgerAccountRepository balances) {
        return new LedgerApplicationService(mock(LedgerAtomicWritePort.class), balances,
            mock(JournalRepository.class), mock(PostingRepository.class),
            id -> new AccountAccessSnapshot(id, owner, "KRW", "ACTIVE"));
    }
    @Test void anotherUserCannotReadAnExistingBalance() {
        var balances = mock(LedgerAccountRepository.class);
        when(balances.findByExternalAccountId(account)).thenReturn(Optional.of(
            new LedgerAccount(UUID.randomUUID(), account, Money.of(new BigDecimal("1000"), Currency.getInstance("KRW")), 0L)));
        var error = assertThrows(ApplicationException.class, () -> service(balances)
            .getBalance(new GetBalanceCommand(UUID.randomUUID(), UUID.randomUUID(), account)));
        assertEquals(ErrorType.FORBIDDEN, error.errorCode().type());
    }
    @Test void anOwnedNewAccountHasZeroBalanceWithoutCreatingLedgerRows() {
        var balances = mock(LedgerAccountRepository.class);
        when(balances.findByExternalAccountId(account)).thenReturn(Optional.empty());
        var result = service(balances).getBalance(new GetBalanceCommand(UUID.randomUUID(), owner, account));
        assertEquals(0, result.balance().compareTo(BigDecimal.ZERO));
        assertEquals("KRW", result.currency());
    }
}

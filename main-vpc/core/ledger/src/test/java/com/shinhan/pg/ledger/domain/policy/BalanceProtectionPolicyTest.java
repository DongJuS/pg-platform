package com.shinhan.pg.ledger.domain.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
import com.shinhan.pg.ledger.domain.error.LedgerException;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/** [MVP1-F19] 음수 잔액 금지 불변식. 호출자는 FOR UPDATE로 잠근 최신 잔액을 전달해야 한다. */
class BalanceProtectionPolicyTest {
    private final BalanceProtectionPolicy policy = new BalanceProtectionPolicy();

    @Test
    void creditAlwaysPasses() {
        assertDoesNotThrow(
                () ->
                        policy.verifyAvailableBalance(
                                account(0),
                                Money.of(new BigDecimal("100"), Currency.getInstance("KRW"))));
    }

    @Test
    void debitWithinBalancePasses() {
        assertDoesNotThrow(() -> policy.verifyAvailableBalance(account(1000), debit("1000")));
        assertDoesNotThrow(() -> policy.verifyAvailableBalance(account(1000), debit("999")));
    }

    @Test
    void overdraftIsRejected() {
        var error =
                assertThrows(
                        LedgerException.class,
                        () -> policy.verifyAvailableBalance(account(1000), debit("1001")));
        assertEquals(LedgerErrorCode.INSUFFICIENT_FUNDS, error.code());
    }

    @Test
    void debitOnZeroBalanceIsRejected() {
        var error =
                assertThrows(
                        LedgerException.class,
                        () -> policy.verifyAvailableBalance(account(0), debit("1")));
        assertEquals(LedgerErrorCode.INSUFFICIENT_FUNDS, error.code());
    }

    @Test
    void currencyMismatchIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        policy.verifyAvailableBalance(
                                account(1000),
                                Money.of(new BigDecimal("-1"), Currency.getInstance("USD"))));
    }

    private static LedgerAccount account(long balanceWon) {
        return new LedgerAccount(
                UUID.randomUUID(), UUID.randomUUID(), Money.of(balanceWon), 0L);
    }

    private static Money debit(String amount) {
        return Money.of(new BigDecimal("-" + amount), Currency.getInstance("KRW"));
    }
}

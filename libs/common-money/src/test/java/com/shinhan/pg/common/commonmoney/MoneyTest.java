package com.shinhan.pg.common.commonmoney;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MoneyTest {
    @Test void exactImmutableAddition() {
        var left = Money.of(new BigDecimal("0.10"), Currency.getInstance("USD"));
        var right = Money.of(new BigDecimal("0.20"), Currency.getInstance("USD"));
        assertEquals(new BigDecimal("0.30"), left.plus(right).amount());
        assertEquals(new BigDecimal("0.10"), left.amount());
    }
    @Test void refusesCurrencyMixingAndImplicitRounding() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(1).plus(Money.of(BigDecimal.ONE,Currency.getInstance("USD"))));
        assertThrows(ArithmeticException.class, () -> Money.of(new BigDecimal("1.001"),Currency.getInstance("USD")));
    }
}

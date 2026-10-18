package com.shinhan.pg.common.commonmoney;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
public final class Money {
    // Do not silently invent FX/fee rounding. Exact currency-scale conversion is required.
    private static final RoundingMode ROUNDING = RoundingMode.UNNECESSARY;
    private final BigDecimal amount;
    private final Currency currency;
    private Money(BigDecimal amount, Currency currency) {
        this.currency = Objects.requireNonNull(currency);
        if (currency.getDefaultFractionDigits() < 0) throw new IllegalArgumentException("Unsupported currency scale");
        this.amount = Objects.requireNonNull(amount).setScale(currency.getDefaultFractionDigits(), ROUNDING);
    }
    public static Money of(long won) { return of(BigDecimal.valueOf(won), Currency.getInstance("KRW")); }
    public static Money of(BigDecimal amount, Currency currency) { return new Money(amount, currency); }
    public Money plus(Money other) {
        Objects.requireNonNull(other);
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
        return of(amount.add(other.amount), currency);
    }
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }
    @Override public boolean equals(Object value) { return value instanceof Money other && amount.equals(other.amount) && currency.equals(other.currency); }
    @Override public int hashCode() { return Objects.hash(amount, currency); }
    @Override public String toString() { return currency.getCurrencyCode()+" "+amount.toPlainString(); }
}

package com.shinhan.pg.payment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.Payment;
import com.shinhan.pg.payment.domain.policy.RefundLimitPolicy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

class RefundLimitPolicyTest {
    private final RefundLimitPolicy policy = new RefundLimitPolicy();

    @Test
    void reservationsConsumeCapacityBeforeThePgResponseArrives() {
        var payment = payment(Money.of(100), Money.of(30), Money.of(50));
        assertDoesNotThrow(() -> policy.verifyRefundAmount(payment, Money.of(20)));
        var error =
                assertThrows(
                        PaymentException.class,
                        () -> policy.verifyRefundAmount(payment, Money.of(21)));
        assertEquals(PaymentErrorCode.REFUND_LIMIT_EXCEEDED, error.code());
    }

    @Test
    void fullyReservedApprovalCannotBeRefundedAgain() {
        var payment = payment(Money.of(100), Money.of(0), Money.of(100));
        assertEquals(
                PaymentErrorCode.REFUND_LIMIT_EXCEEDED,
                assertThrows(
                                PaymentException.class,
                                () -> policy.verifyRefundAmount(payment, Money.of(1)))
                        .code());
    }

    @Test
    void rejectsNonPositiveRequests() {
        var payment = payment(Money.of(100), Money.of(0), Money.of(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.verifyRefundAmount(payment, Money.of(0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.verifyRefundAmount(payment, Money.of(-1)));
    }

    @Test
    void rejectsMixedCurrenciesInRequestAndPersistedTotals() {
        var usd = Money.of(new BigDecimal("0.01"), Currency.getInstance("USD"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        policy.verifyRefundAmount(
                                payment(Money.of(100), Money.of(0), Money.of(0)), usd));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        policy.verifyRefundAmount(
                                payment(usd, Money.of(0), Money.of(0)), Money.of(1)));
    }

    @Test
    void exactFractionalCurrencyBoundary() {
        var usd = Currency.getInstance("USD");
        var payment =
                payment(
                        Money.of(new BigDecimal("1.00"), usd),
                        Money.of(new BigDecimal("0.33"), usd),
                        Money.of(new BigDecimal("0.66"), usd));
        assertDoesNotThrow(
                () -> policy.verifyRefundAmount(payment, Money.of(new BigDecimal("0.01"), usd)));
        assertThrows(
                PaymentException.class,
                () -> policy.verifyRefundAmount(payment, Money.of(new BigDecimal("0.02"), usd)));
    }

    @Test
    void rejectsCorruptNegativeReservationsAndNonPositiveApprovals() {
        assertEquals(
                PaymentErrorCode.INVALID_STATE,
                assertThrows(
                                PaymentException.class,
                                () ->
                                        policy.verifyRefundAmount(
                                                payment(Money.of(100), Money.of(0), Money.of(-1)),
                                                Money.of(1)))
                        .code());
        assertThrows(
                PaymentException.class,
                () ->
                        policy.verifyRefundAmount(
                                payment(Money.of(0), Money.of(0), Money.of(0)), Money.of(1)));
    }

    private Payment payment(Money approved, Money refunded, Money reserved) {
        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "synthetic-order",
                approved,
                refunded,
                reserved,
                "APPROVED");
    }
}

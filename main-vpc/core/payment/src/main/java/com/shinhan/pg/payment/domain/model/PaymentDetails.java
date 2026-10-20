package com.shinhan.pg.payment.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.util.UUID;

/** Current payment aggregate; provider outcome and ledger completion remain separate. */
public record PaymentDetails(
        UUID id,
        UUID actorUserId,
        UUID merchantId,
        String orderReference,
        UUID sourceAccountId,
        UUID destinationAccountId,
        Money amount,
        Money refunded,
        Money reserved,
        String status,
        long version) {
    public Payment limitView() {
        return new Payment(
                id, actorUserId, merchantId, orderReference, amount, refunded, reserved, status);
    }
}

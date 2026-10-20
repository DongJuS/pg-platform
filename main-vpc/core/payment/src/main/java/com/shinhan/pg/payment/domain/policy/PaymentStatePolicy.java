package com.shinhan.pg.payment.domain.policy;

import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.Payment;

import java.util.Set;

/**
 * F15: final internal states never move backwards; provider version ordering is operation-scoped.
 */
public final class PaymentStatePolicy {
    public void validateTransition(Payment payment, String nextStatus, String externalEventId) {
        if (payment == null
                || nextStatus == null
                || externalEventId == null
                || externalEventId.isBlank())
            throw new PaymentException(PaymentErrorCode.INVALID_STATE);
        if (payment.status().equals(nextStatus)) return;
        Set<String> allowed =
                switch (payment.status()) {
                    case "PROCESSING" -> Set.of("UNKNOWN", "APPROVED", "DECLINED");
                    case "UNKNOWN" -> Set.of("APPROVED", "DECLINED");
                    case "APPROVED" -> Set.of("PARTIALLY_REFUNDED", "REFUNDED", "CANCELLED");
                    case "PARTIALLY_REFUNDED" -> Set.of("REFUNDED");
                    default -> Set.of();
                };
        if (!allowed.contains(nextStatus))
            throw new PaymentException(PaymentErrorCode.INVALID_STATE);
    }
}

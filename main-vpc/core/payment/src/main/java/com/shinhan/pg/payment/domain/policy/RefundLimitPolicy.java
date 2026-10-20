package com.shinhan.pg.payment.domain.policy;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.payment.domain.error.PaymentErrorCode;
import com.shinhan.pg.payment.domain.error.PaymentException;
import com.shinhan.pg.payment.domain.model.Payment;

import java.util.Objects;

/**
 *
 *
 * <pre>
 * [MVP2-F16] 확정액 + 처리 중 예약액 + 신규 요청액 <= 승인액을 검증한다.
 * DB 잠금으로 읽은 최신 Payment에 적용하고 같은 로컬 트랜잭션에서 예약해야 한다.
 * 이 순수 계산만으로 동시성 제어를 보장하지 않는다. 취소 충돌 우선순위는 별도 정책이다.
 * 근거: Figma 9:210, 사용자 확정 환불 한도 기준.
 * </pre>
 */
public final class RefundLimitPolicy {
    public void verifyRefundAmount(Payment payment, Money requestedRefund) {
        Objects.requireNonNull(payment);
        Objects.requireNonNull(requestedRefund);
        if (requestedRefund.amount().signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }
        if (payment.approvedAmount().amount().signum() <= 0
                || payment.refundedAmount().amount().signum() < 0
                || payment.reservedRefundAmount().amount().signum() < 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_STATE);
        }
        // Money.plus checks currency compatibility and retains exact currency precision.
        var total =
                payment.refundedAmount().plus(payment.reservedRefundAmount()).plus(requestedRefund);
        if (!total.currency().equals(payment.approvedAmount().currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        if (total.amount().compareTo(payment.approvedAmount().amount()) > 0) {
            throw new PaymentException(PaymentErrorCode.REFUND_LIMIT_EXCEEDED);
        }
    }
}

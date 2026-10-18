package com.shinhan.pg.payment.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.payment.domain.model.Payment;

/**
 * <pre>
 * [MVP2-F16] 동시 환불·취소를 조정하고 확정/예약 환불액 합계가 승인액을 넘지 않게 한다. 충돌 우선순위는 미정.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class RefundLimitPolicy {
    public void verifyRefundAmount(Payment payment, Money requestedRefund) {
        throw new FeatureNotImplementedException("MVP2-F16");
    }
}

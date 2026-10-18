package com.shinhan.pg.payment.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.domain.model.Payment;

/**
 * <pre>
 * [MVP2-F15] 승인 응답·웹훅의 중복/역순 전이를 같은 거래 효과로 수렴시킨다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class PaymentStatePolicy {
    public void validateTransition(Payment payment, String nextStatus, String externalEventId) {
        throw new FeatureNotImplementedException("MVP2-F15");
    }
}

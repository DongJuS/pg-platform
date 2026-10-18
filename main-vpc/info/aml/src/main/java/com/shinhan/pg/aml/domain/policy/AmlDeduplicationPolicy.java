package com.shinhan.pg.aml.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.UUID;

/**
 * <pre>
 * [MVP6-F05] ledger.posted와 transfer.completed가 같은 거래 효과를 두 번 집계하지 않게 한다. eventId와 업무 effectKey를 구분한다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class AmlDeduplicationPolicy {
    public String effectKey(UUID transactionId, String ruleVersion, String windowKey) {
        throw new FeatureNotImplementedException("MVP6-F05");
    }
}

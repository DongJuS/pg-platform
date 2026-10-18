package com.shinhan.pg.fepadapter.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.fepadapter.domain.model.FepMessage;

/**
 * <pre>
 * [MVP5-F04,MVP7-F03] channelCode+messageKey를 중복 효과 키로 사용한다. 금융 FEP와 외부 LLM 중계를 합치지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class InstitutionMessageIdempotencyPolicy {
    public void validateReplay(FepMessage previous, String messageHash) {
        throw new FeatureNotImplementedException("MVP5-F04");
    }
}

package com.shinhan.pg.settlement.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.settlement.domain.model.SettlementCycle;

/**
 * <pre>
 * [MVP7-F08] 회차의 업무일·시간대·cutoff·입력 버전을 고정한다. 지연 거래 포함/이월 기준은 미정.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class SettlementCutoffPolicy {
    public void validateInputVersion(SettlementCycle cycle, String currentInputVersion) {
        throw new FeatureNotImplementedException("MVP7-F08");
    }
}

package com.shinhan.pg.settlement.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.settlement.domain.model.SettlementCycle;

/**
 * <pre>
 * [MVP7-F07] 생성·송신·수신·대사·원장 반영·확정을 기록한다. 같은 회차의 응답 유실에서 중복 분개하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class SettlementRecoveryPolicy {
    public void validateResume(SettlementCycle cycle, String checkpoint) {
        throw new FeatureNotImplementedException("MVP7-F07");
    }
}

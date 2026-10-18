package com.shinhan.pg.wealthreport.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import java.time.Instant;

/**
 * <pre>
 * [MVP4-F09] 같은 자료 시점·거래 효과·버전을 사용한다. 지연 이벤트 허용 범위·재구축 포함 기준은 미정.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class SnapshotConsistencyPolicy {
    public void validateSnapshot(AssetSnapshot snapshot, Instant requestedAsOf) {
        throw new FeatureNotImplementedException("MVP4-F09");
    }
}

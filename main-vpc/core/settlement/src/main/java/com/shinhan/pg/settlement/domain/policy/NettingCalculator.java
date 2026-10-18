package com.shinhan.pg.settlement.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.settlement.domain.model.NettingDraft;
import com.shinhan.pg.settlement.domain.model.SettlementLine;

import java.util.List;

/**
 * <pre>
 * [MVP7-F02] 기관별 보낼 금액-받을 금액·전체 합계 0 검증 위치. 수수료·환율·상세 산식은 후속.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class NettingCalculator {
    public NettingDraft calculate(List<SettlementLine> lines) {
        throw new FeatureNotImplementedException("MVP7-F02");
    }
}

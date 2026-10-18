package com.shinhan.pg.wealthreport.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;

/**
 * <pre>
 * [MVP4-F08] Java에서 지표와 계산 버전·asOf를 확정한다. 금융 산식·반올림 상세는 미정이므로 수치를 생성하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class FinancialMetricsCalculator {
    public FinancialMetrics calculate(AssetSnapshot snapshot) {
        throw new FeatureNotImplementedException("MVP4-F08");
    }
}

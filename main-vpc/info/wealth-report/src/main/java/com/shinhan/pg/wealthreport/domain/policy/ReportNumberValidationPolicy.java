package com.shinhan.pg.wealthreport.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;

/**
 * <pre>
 * [MVP4-F07] AI 초안의 숫자·통화·기준 시점을 계산 결과와 대조한다. 검증 실패한 PDF를 고객에게 전달하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class ReportNumberValidationPolicy {
    public void validateNumbers(FinancialMetrics calculated, String aiDraft) {
        throw new FeatureNotImplementedException("MVP4-F07");
    }
}

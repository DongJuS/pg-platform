package com.shinhan.pg.aml.domain.policy;

import com.shinhan.pg.aml.domain.model.AmlCase;
import com.shinhan.pg.aml.domain.model.DetectionRule;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.List;

/**
 * <pre>
 * [MVP6-F04] 룰 점수 기준으로 자동 보고 대상을 판정한다. 사람 승인 단계를 요구하지 않는다. 임계값·오탐 정책은 미정.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class AmlReportingDecisionPolicy {
    public boolean isReportRequired(AmlCase amlCase, List<DetectionRule> rules) {
        throw new FeatureNotImplementedException("MVP6-F04");
    }
}

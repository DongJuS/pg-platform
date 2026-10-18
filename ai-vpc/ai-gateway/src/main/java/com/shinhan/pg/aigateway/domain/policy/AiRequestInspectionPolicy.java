package com.shinhan.pg.aigateway.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP3-F01] 입력/출력 PII·정책 위반 판정 위치. 탐지 모델·기준·사용량 한도는 확정하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class AiRequestInspectionPolicy {
    public void inspectSanitizedInput(String sanitizedPrompt) {
        throw new FeatureNotImplementedException("MVP3-F01");
    }
}

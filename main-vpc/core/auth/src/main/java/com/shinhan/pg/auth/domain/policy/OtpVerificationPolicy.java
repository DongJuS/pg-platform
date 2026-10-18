package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.auth.domain.model.OtpProof;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.time.Instant;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F06] Mock OTP의 실패·잠금·proof 일회 소모 규칙을 정의한다. 실제 OTP 공급자를 호출하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class OtpVerificationPolicy {
    public void validateProof(OtpProof proof, UUID transferDraftId, Instant now) {
        throw new FeatureNotImplementedException("MVP1-F06");
    }
}

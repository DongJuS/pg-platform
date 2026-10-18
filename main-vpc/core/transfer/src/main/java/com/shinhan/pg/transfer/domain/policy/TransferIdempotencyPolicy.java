package com.shinhan.pg.transfer.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.domain.model.Transfer;

/**
 * <pre>
 * [MVP1-F15,MVP1-F20] actorUserId+idempotencyKey·본문 해시를 비교한다. 같은 키 요청은 409. 원장 재조회 복구와 신규 요청을 구분한다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class TransferIdempotencyPolicy {
    public void validateDuplicateRequest(Transfer original, String requestHash) {
        throw new FeatureNotImplementedException("MVP1-F15");
    }
}

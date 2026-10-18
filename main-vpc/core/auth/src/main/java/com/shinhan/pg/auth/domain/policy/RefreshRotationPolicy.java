package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.auth.domain.model.RefreshSession;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.time.Instant;

/**
 * <pre>
 * [MVP1-F03] refresh 해시 조회·회전·재사용 탐지 상태를 원자적으로 바꿀 조건을 정의한다. 기본 로그아웃 보완은 보류.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class RefreshRotationPolicy {
    public void validateRotation(RefreshSession current, Instant now) {
        throw new FeatureNotImplementedException("MVP1-F03");
    }
}

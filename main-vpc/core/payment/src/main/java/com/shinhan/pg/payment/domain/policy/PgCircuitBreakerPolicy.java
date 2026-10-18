package com.shinhan.pg.payment.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.time.Instant;

/**
 * <pre>
 * [MVP2-F07] FEP 경유 PG 통신의 실패/지연 차단 위치. Resilience4j 도입·임계값·fallback 계약은 미정이며 가짜 승인으로 대체하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class PgCircuitBreakerPolicy {
    public boolean permitsAttempt(int failureCount, Instant lastFailureAt, Instant now) {
        throw new FeatureNotImplementedException("MVP2-F07");
    }
}

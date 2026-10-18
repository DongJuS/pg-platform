package com.shinhan.pg.ledger.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.ledger.domain.model.Journal;

/**
 * <pre>
 * [MVP1-F13] 분개가 통화별 합계 0인지 검사한다. 부분 반영을 허용하지 않는다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class JournalBalancePolicy {
    public void validateBalanced(Journal journal) {
        throw new FeatureNotImplementedException("MVP1-F13");
    }
}

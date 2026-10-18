package com.shinhan.pg.account.domain.policy;

import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP1-F10] ACTIVE/SUSPENDED 허용 전이와 변경 사유를 검증할 위치. 변경 권한의 최종 계약은 미정.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class AccountStatusPolicy {
    public void validateTransition(Account account, String nextStatus) {
        throw new FeatureNotImplementedException("MVP1-F10");
    }
}

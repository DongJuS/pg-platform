package com.shinhan.pg.account.domain.policy;

import com.shinhan.pg.account.domain.error.AccountErrorCode;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.ApplicationException;

/**
 * <pre>
 * [MVP1-F10] ACTIVE/SUSPENDED 허용 전이와 변경 사유를 검증할 위치. 변경 권한의 최종 계약은 미정.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 *
 * <p>구현 확정: 허용 전이는 ACTIVE↔SUSPENDED 뿐이다. 동일 상태로의 변경, 알 수 없는 상태 값은
 * 모두 INVALID_STATUS_TRANSITION(409)이다. 변경 사유(reason)의 필수성·형식은 미정 — 여기서 검증하지 않는다.
 */
public final class AccountStatusPolicy {
    public void validateTransition(Account account, String nextStatus) {
        boolean allowed =
                (Account.STATUS_ACTIVE.equals(account.status())
                                && Account.STATUS_SUSPENDED.equals(nextStatus))
                        || (Account.STATUS_SUSPENDED.equals(account.status())
                                && Account.STATUS_ACTIVE.equals(nextStatus));
        if (!allowed) {
            throw new ApplicationException(AccountErrorCode.INVALID_STATUS_TRANSITION);
        }
    }
}

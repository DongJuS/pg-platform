package com.shinhan.pg.ledger.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

/**
 * <pre>
 * [MVP1-F19] 계좌 ID 정렬 잠금 안에서 잔액 조건·분개·갱신을 하나로 보호한다. DB 트랜잭션 구현은 adapter 책임.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class BalanceProtectionPolicy {
    public void verifyAvailableBalance(LedgerAccount account, Money requestedDebit) {
        throw new FeatureNotImplementedException("MVP1-F19");
    }
}

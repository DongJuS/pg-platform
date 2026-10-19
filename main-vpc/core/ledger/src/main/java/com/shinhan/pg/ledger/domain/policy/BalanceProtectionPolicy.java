package com.shinhan.pg.ledger.domain.policy;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;
import com.shinhan.pg.ledger.domain.error.LedgerException;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

import java.util.Objects;

/**
 * <pre>
 * [MVP1-F19] 계좌 ID 정렬 잠금 안에서 잔액 조건·분개·갱신을 하나로 보호한다. DB 트랜잭션 구현은 adapter 책임.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class BalanceProtectionPolicy {
    /**
     * 음수 잔액 금지 불변식. {@code requestedDebit}은 해당 계좌에 적용할 부호 있는 금액
     * (음수 = 출금)이다. 적용 결과 잔액이 음수가 되면 LedgerException(INSUFFICIENT_FUNDS).
     * 호출자는 반드시 계좌 행을 FOR UPDATE로 잠근 뒤 최신 잔액으로 호출해야 한다
     * (잔액 스냅샷 캐시로 이 검사를 우회하지 않는다 — ADR 0001 6-A).
     */
    public void verifyAvailableBalance(LedgerAccount account, Money requestedDebit) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(requestedDebit, "requestedDebit");
        if (!account.balance().currency().equals(requestedDebit.currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        if (requestedDebit.amount().signum() < 0
                && account.balance().amount().add(requestedDebit.amount()).signum() < 0) {
            throw new LedgerException(
                    LedgerErrorCode.INSUFFICIENT_FUNDS,
                    "insufficient funds: ledgerAccountId=" + account.id());
        }
    }
}

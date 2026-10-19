package com.shinhan.pg.ledger.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.adapter.out.persistence.entity.LedgerAccountEntity;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

import java.util.Currency;

/** [MVP1-F11,MVP1-F19] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class LedgerAccountEntityMapper {
    public LedgerAccount toDomain(LedgerAccountEntity entity) {
        /* 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        return new LedgerAccount(
                entity.id(),
                entity.externalAccountId(),
                Money.of(entity.balance(), Currency.getInstance(entity.balanceCurrency())),
                entity.version());
    }

    public LedgerAccountEntity toEntity(LedgerAccount value) {
        /* SQL 매핑: balance는 통화 scale이 보장된 Money 금액만 저장한다 */
        return new LedgerAccountEntity(
                value.id(),
                value.externalAccountId(),
                value.balance().amount(),
                value.balance().currency().getCurrencyCode(),
                value.version());
    }
}

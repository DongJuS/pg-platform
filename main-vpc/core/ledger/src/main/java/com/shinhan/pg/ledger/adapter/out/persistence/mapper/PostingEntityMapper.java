package com.shinhan.pg.ledger.adapter.out.persistence.mapper;

import com.shinhan.pg.common.commonmoney.Money;
import com.shinhan.pg.ledger.adapter.out.persistence.entity.PostingEntity;
import com.shinhan.pg.ledger.domain.model.Posting;

import java.util.Currency;

/** [MVP1-F12,MVP1-F13] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class PostingEntityMapper {
    public Posting toDomain(PostingEntity entity) {
        /* 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        return new Posting(
                entity.id(),
                entity.journalId(),
                entity.ledgerAccountId(),
                Money.of(
                        entity.signedAmount(),
                        Currency.getInstance(entity.signedAmountCurrency())));
    }

    public PostingEntity toEntity(Posting value) {
        /* SQL 매핑: signedAmount는 통화 scale이 보장된 Money 금액만 저장한다 */
        return new PostingEntity(
                value.id(),
                value.journalId(),
                value.ledgerAccountId(),
                value.signedAmount().amount(),
                value.signedAmount().currency().getCurrencyCode());
    }
}

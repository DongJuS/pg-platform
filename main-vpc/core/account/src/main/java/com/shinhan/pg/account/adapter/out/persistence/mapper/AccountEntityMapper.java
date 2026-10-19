package com.shinhan.pg.account.adapter.out.persistence.mapper;

import com.shinhan.pg.account.adapter.out.persistence.entity.AccountEntity;
import com.shinhan.pg.account.domain.model.Account;

/** [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class AccountEntityMapper {
    public Account toDomain(AccountEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환
           — 확정: createdAt은 도메인에 노출하지 않고, 나머지 필드는 1:1 변환한다. */
        return new Account(
                entity.id(),
                entity.ownerId(),
                entity.accountNumber(),
                entity.currency(),
                entity.alias(),
                entity.status());
    }

    public AccountEntity toEntity(Account value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현
           — 확정: SQL row mapping. createdAt은 insert 시 DB 기본값이 채우므로 여기서는 null을 둔다. */
        return new AccountEntity(
                value.id(),
                value.ownerId(),
                value.accountNumber(),
                value.currency(),
                value.alias(),
                value.status(),
                null);
    }
}

package com.shinhan.pg.account.adapter.out.persistence.mapper;

import com.shinhan.pg.account.adapter.out.persistence.entity.AccountEntity;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 저장 모델과 순수 도메인 모델의 변환. 업무 판정·외부 호출을 넣지 않는다. */
public final class AccountEntityMapper {
    public Account toDomain(AccountEntity entity) {
        /* TODO: 누락 값·금액/통화·시간대·관계를 명시적으로 변환 */
        throw new FeatureNotImplementedException("MVP1-F07");
    }

    public AccountEntity toEntity(Account value) {
        /* TODO: ORM/SQL 매핑 확정 후 구현 */
        throw new FeatureNotImplementedException("MVP1-F07");
    }
}

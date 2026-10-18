package com.shinhan.pg.account.adapter.out.persistence;

import com.shinhan.pg.account.application.port.out.AccountRepository;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] Account 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 AccountEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class AccountPersistenceAdapter implements AccountRepository {
    @Override
    public Optional<Account> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP1-F07");
    }

    @Override
    public void save(Account value) {
        throw new FeatureNotImplementedException("MVP1-F07");
    }
}

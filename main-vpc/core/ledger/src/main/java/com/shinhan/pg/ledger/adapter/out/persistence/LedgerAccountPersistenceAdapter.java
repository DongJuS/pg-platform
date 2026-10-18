package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.ledger.application.port.out.LedgerAccountRepository;
import com.shinhan.pg.ledger.domain.model.LedgerAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F11,MVP1-F19] LedgerAccount 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 LedgerAccountEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class LedgerAccountPersistenceAdapter implements LedgerAccountRepository {
    @Override
    public Optional<LedgerAccount> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP1-F11");
    }

    @Override
    public void save(LedgerAccount value) {
        throw new FeatureNotImplementedException("MVP1-F11");
    }
}

package com.shinhan.pg.payment.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.application.port.out.ReconciliationRunRepository;
import com.shinhan.pg.payment.domain.model.ReconciliationRun;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F08,MVP2-F09] ReconciliationRun 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 ReconciliationRunEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class ReconciliationRunPersistenceAdapter implements ReconciliationRunRepository {
    @Override
    public Optional<ReconciliationRun> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP2-F08");
    }

    @Override
    public void save(ReconciliationRun value) {
        throw new FeatureNotImplementedException("MVP2-F08");
    }
}

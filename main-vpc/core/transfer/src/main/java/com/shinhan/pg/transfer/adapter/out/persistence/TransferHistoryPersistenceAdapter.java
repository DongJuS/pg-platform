package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.application.port.out.TransferHistoryRepository;
import com.shinhan.pg.transfer.domain.model.TransferHistory;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F16] TransferHistory 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 TransferHistoryEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class TransferHistoryPersistenceAdapter implements TransferHistoryRepository {
    @Override
    public Optional<TransferHistory> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP1-F16");
    }

    @Override
    public void save(TransferHistory value) {
        throw new FeatureNotImplementedException("MVP1-F16");
    }
}

package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.application.port.out.TransferRepository;
import com.shinhan.pg.transfer.domain.model.Transfer;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F15,MVP1-F16,MVP1-F17,MVP1-F20] Transfer 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 TransferEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class TransferPersistenceAdapter implements TransferRepository {
    @Override
    public Optional<Transfer> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP1-F15");
    }

    @Override
    public void save(Transfer value) {
        throw new FeatureNotImplementedException("MVP1-F15");
    }

    @Override
    public Optional<Transfer> findByIdempotencyKey(UUID actorUserId, String idempotencyKey) {
        throw new FeatureNotImplementedException("MVP1-F15");
    }
}

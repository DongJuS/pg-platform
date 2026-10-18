package com.shinhan.pg.payment.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.application.port.out.RefundRepository;
import com.shinhan.pg.payment.domain.model.Refund;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F04,MVP2-F05,MVP2-F16] Refund 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 RefundEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class RefundPersistenceAdapter implements RefundRepository {
    @Override
    public Optional<Refund> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP2-F04");
    }

    @Override
    public void save(Refund value) {
        throw new FeatureNotImplementedException("MVP2-F04");
    }
}

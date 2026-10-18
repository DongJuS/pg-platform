package com.shinhan.pg.payment.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.application.port.out.PgEventRepository;
import com.shinhan.pg.payment.domain.model.PgEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F06,MVP2-F15] PgEvent 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 PgEventEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class PgEventPersistenceAdapter implements PgEventRepository {
    @Override
    public Optional<PgEvent> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP2-F06");
    }

    @Override
    public void save(PgEvent value) {
        throw new FeatureNotImplementedException("MVP2-F06");
    }
}

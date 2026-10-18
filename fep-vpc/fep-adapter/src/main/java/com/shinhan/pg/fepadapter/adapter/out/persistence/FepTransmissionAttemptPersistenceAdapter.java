package com.shinhan.pg.fepadapter.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.fepadapter.application.port.out.FepTransmissionAttemptRepository;
import com.shinhan.pg.fepadapter.domain.model.FepTransmissionAttempt;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F04,MVP7-F03] FepTransmissionAttempt 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 FepTransmissionAttemptEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class FepTransmissionAttemptPersistenceAdapter
        implements FepTransmissionAttemptRepository {
    @Override
    public Optional<FepTransmissionAttempt> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP5-F04");
    }

    @Override
    public void save(FepTransmissionAttempt value) {
        throw new FeatureNotImplementedException("MVP5-F04");
    }
}

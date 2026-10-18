package com.shinhan.pg.swift.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.swift.application.port.out.SwiftMessageRepository;
import com.shinhan.pg.swift.domain.model.SwiftMessage;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F03,MVP5-F05,MVP5-F08] SwiftMessage 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 SwiftMessageEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class SwiftMessagePersistenceAdapter implements SwiftMessageRepository {
    @Override
    public Optional<SwiftMessage> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP5-F03");
    }

    @Override
    public void save(SwiftMessage value) {
        throw new FeatureNotImplementedException("MVP5-F03");
    }

    @Override
    public Optional<SwiftMessage> findByUetr(String uetr) {
        throw new FeatureNotImplementedException("MVP5-F03");
    }
}

package com.shinhan.pg.aigateway.adapter.out.persistence;

import com.shinhan.pg.aigateway.application.port.out.AiRequestAuditRepository;
import com.shinhan.pg.aigateway.domain.model.AiRequestAudit;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP3-F01,MVP3-F02] AiRequestAudit 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 AiRequestAuditEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class AiRequestAuditPersistenceAdapter implements AiRequestAuditRepository {
    @Override
    public Optional<AiRequestAudit> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP3-F01");
    }

    @Override
    public void save(AiRequestAudit value) {
        throw new FeatureNotImplementedException("MVP3-F01");
    }
}

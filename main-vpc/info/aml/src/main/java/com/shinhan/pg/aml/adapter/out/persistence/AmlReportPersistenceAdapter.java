package com.shinhan.pg.aml.adapter.out.persistence;

import com.shinhan.pg.aml.application.port.out.AmlReportRepository;
import com.shinhan.pg.aml.domain.model.AmlReport;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP6-F03,MVP6-F05] AmlReport 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 AmlReportEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class AmlReportPersistenceAdapter implements AmlReportRepository {
    @Override
    public Optional<AmlReport> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP6-F03");
    }

    @Override
    public void save(AmlReport value) {
        throw new FeatureNotImplementedException("MVP6-F03");
    }
}

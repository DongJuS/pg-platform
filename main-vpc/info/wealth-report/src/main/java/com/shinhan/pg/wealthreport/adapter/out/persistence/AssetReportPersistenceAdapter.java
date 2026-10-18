package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.application.port.out.AssetReportRepository;
import com.shinhan.pg.wealthreport.domain.model.AssetReport;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP4-F01,MVP4-F02,MVP4-F03,MVP4-F10] AssetReport 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 AssetReportEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class AssetReportPersistenceAdapter implements AssetReportRepository {
    @Override
    public Optional<AssetReport> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP4-F01");
    }

    @Override
    public void save(AssetReport value) {
        throw new FeatureNotImplementedException("MVP4-F01");
    }
}

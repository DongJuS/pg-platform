package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.application.port.out.AssetSnapshotRepository;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP4-F05,MVP4-F09] AssetSnapshot 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 AssetSnapshotEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class AssetSnapshotPersistenceAdapter implements AssetSnapshotRepository {
    @Override
    public Optional<AssetSnapshot> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP4-F05");
    }

    @Override
    public void save(AssetSnapshot value) {
        throw new FeatureNotImplementedException("MVP4-F05");
    }
}

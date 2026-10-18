package com.shinhan.pg.settlement.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.settlement.application.port.out.SettlementFileRepository;
import com.shinhan.pg.settlement.domain.model.SettlementFile;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP7-F02,MVP7-F03,MVP7-F07] SettlementFile 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 SettlementFileEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class SettlementFilePersistenceAdapter implements SettlementFileRepository {
    @Override
    public Optional<SettlementFile> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP7-F02");
    }

    @Override
    public void save(SettlementFile value) {
        throw new FeatureNotImplementedException("MVP7-F02");
    }
}

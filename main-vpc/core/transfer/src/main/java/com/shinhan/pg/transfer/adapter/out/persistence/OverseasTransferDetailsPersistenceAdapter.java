package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferDetailsRepository;
import com.shinhan.pg.transfer.domain.model.OverseasTransferDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP5-F01,MVP5-F02,MVP5-F08] OverseasTransferDetails 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 OverseasTransferDetailsEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class OverseasTransferDetailsPersistenceAdapter
        implements OverseasTransferDetailsRepository {
    @Override
    public Optional<OverseasTransferDetails> findByTransferId(UUID transferId) {
        throw new FeatureNotImplementedException("MVP5-F01");
    }

    @Override
    public void save(OverseasTransferDetails value) {
        throw new FeatureNotImplementedException("MVP5-F01");
    }
}

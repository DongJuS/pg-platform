package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.wealthreport.application.port.out.TokenMappingRepository;
import com.shinhan.pg.wealthreport.domain.model.TokenMapping;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP4-F06] TokenMapping 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 TokenMappingEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class TokenMappingPersistenceAdapter implements TokenMappingRepository {
    @Override
    public Optional<TokenMapping> findByReportAndToken(UUID reportId, String tokenLabel) {
        throw new FeatureNotImplementedException("MVP4-F06");
    }

    @Override
    public void save(TokenMapping value) {
        throw new FeatureNotImplementedException("MVP4-F06");
    }
}

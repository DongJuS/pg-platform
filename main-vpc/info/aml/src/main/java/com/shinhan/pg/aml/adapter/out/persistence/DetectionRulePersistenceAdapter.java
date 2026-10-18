package com.shinhan.pg.aml.adapter.out.persistence;

import com.shinhan.pg.aml.application.port.out.DetectionRuleRepository;
import com.shinhan.pg.aml.domain.model.DetectionRule;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.Optional;

/**
 * <pre>
 * [MVP6-F01,MVP6-F04] DetectionRule 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 DetectionRuleEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class DetectionRulePersistenceAdapter implements DetectionRuleRepository {
    @Override
    public Optional<DetectionRule> findByCodeAndVersion(String code, String version) {
        throw new FeatureNotImplementedException("MVP6-F01");
    }

    @Override
    public void save(DetectionRule value) {
        throw new FeatureNotImplementedException("MVP6-F01");
    }
}

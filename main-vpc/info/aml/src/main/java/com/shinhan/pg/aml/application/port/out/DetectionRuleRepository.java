package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.domain.model.DetectionRule;

import java.util.Optional;

/**
 * <pre>
 * [MVP6-F01,MVP6-F04] 규칙·임계값·집계 창은 미정. 실제 CTR 규제 준수나 실제 현금 거래로 표방하지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface DetectionRuleRepository {
    Optional<DetectionRule> findByCodeAndVersion(String code, String version);

    void save(DetectionRule value);
}

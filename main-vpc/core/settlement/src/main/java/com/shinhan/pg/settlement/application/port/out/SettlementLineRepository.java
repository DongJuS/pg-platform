package com.shinhan.pg.settlement.application.port.out;

import com.shinhan.pg.settlement.domain.model.SettlementLine;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP7-F02,MVP7-F08] 거래 효과를 회차에 한 번만 포함한다. 지연 거래 이월·보정 정책은 보류다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface SettlementLineRepository {
    Optional<SettlementLine> findById(UUID id);

    void save(SettlementLine value);
}

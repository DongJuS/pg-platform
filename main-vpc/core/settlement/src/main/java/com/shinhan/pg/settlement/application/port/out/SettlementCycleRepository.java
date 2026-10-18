package com.shinhan.pg.settlement.application.port.out;

import com.shinhan.pg.settlement.domain.model.SettlementCycle;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP7-F01,MVP7-F04,MVP7-F07,MVP7-F08] 업무일 외에 회차·버전·고정 입력을 보존한다. 재실행이 입력 집합을 조용히 바꾸지 않게 한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface SettlementCycleRepository {
    Optional<SettlementCycle> findById(UUID id);

    void save(SettlementCycle value);
}

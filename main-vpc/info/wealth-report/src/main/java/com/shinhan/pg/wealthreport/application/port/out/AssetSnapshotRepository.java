package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP4-F05,MVP4-F09] 지연·중복 거래를 처리할 자료 기준을 고정한다. 실시간 잔액을 섞지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface AssetSnapshotRepository {
    Optional<AssetSnapshot> findById(UUID id);

    void save(AssetSnapshot value);
}

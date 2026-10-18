package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.domain.model.TransferHistory;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F16] 전이와 사유·시점을 보존한다. 최종 상태의 역전·중복 완료를 막을 위치.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface TransferHistoryRepository {
    Optional<TransferHistory> findById(UUID id);

    void save(TransferHistory value);
}

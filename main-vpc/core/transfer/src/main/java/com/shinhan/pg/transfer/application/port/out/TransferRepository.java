package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.domain.model.Transfer;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F15,MVP1-F16,MVP1-F17,MVP1-F20] 중복 요청은 409. UNKNOWN을 실패로 덮지 않고 같은 transactionId로 확인한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface TransferRepository {
    Optional<Transfer> findById(UUID id);

    void save(Transfer value);

    Optional<Transfer> findByIdempotencyKey(UUID actorUserId, String idempotencyKey);
}

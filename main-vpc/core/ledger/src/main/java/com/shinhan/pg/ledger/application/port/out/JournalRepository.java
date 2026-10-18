package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.domain.model.Journal;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F13,MVP1-F14] 동일 transactionId의 분개는 한 번만 반영. 통화별 posting 합계 0·부분 반영 없음.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface JournalRepository {
    Optional<Journal> findById(UUID id);

    void save(Journal value);

    Optional<Journal> findByTransactionId(UUID transactionId);
}

package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.domain.model.LedgerAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F11,MVP1-F19] 계좌 ID 순서 잠금과 잔액 갱신의 권위자. Redis 보조 락으로 DB 원자성을 대체하지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface LedgerAccountRepository {
    Optional<LedgerAccount> findById(UUID id);

    void save(LedgerAccount value);
}

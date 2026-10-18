package com.shinhan.pg.account.application.port.out;

import com.shinhan.pg.account.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 계좌 메타데이터만 소유한다. 잔액은 Ledger에서 조회한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface AccountRepository {
    Optional<Account> findById(UUID id);

    void save(Account value);
}

package com.shinhan.pg.account.application.port.out;

import com.shinhan.pg.account.domain.model.Account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F07,MVP1-F08,MVP1-F09,MVP1-F10] 계좌 메타데이터만 소유한다. 잔액은 Ledger에서 조회한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 *
 * <p>구현 확정: 스키마는 V3__accounts.sql. 부재 시 Optional.empty(404는 application이 매핑).
 * 목록 조회는 owner_id + created_at 인덱스 경로로 offset/limit 페이지네이션한다.
 */
public interface AccountRepository {
    Optional<Account> findById(UUID id);

    /** [MVP1-F08] 본인 계좌만 최신순(created_at desc, id desc)으로 반환한다. */
    List<Account> findByOwnerId(UUID ownerId, int offset, int limit);

    void save(Account value);
}

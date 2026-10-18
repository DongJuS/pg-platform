package com.shinhan.pg.auth.application.port.out;

import com.shinhan.pg.auth.domain.model.RefreshSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP1-F03] 회전·재사용 감지의 원자성을 정의한다. 기본 로그아웃의 refresh 전체 철회는 보류다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface RefreshSessionRepository {
    Optional<RefreshSession> findById(UUID id);

    void save(RefreshSession value);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    /** 재사용 탐지 시 family의 전체 세션을 철회한다. */
    void revokeFamily(UUID familyId, Instant revokedAt);
}

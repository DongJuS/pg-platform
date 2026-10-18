package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.domain.model.PgEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F06,MVP2-F15] 서명 검증 뒤 기록한다. 동일 ID의 다른 내용·역순 전이 정책은 미정이다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface PgEventRepository {
    Optional<PgEvent> findById(UUID id);

    void save(PgEvent value);
}

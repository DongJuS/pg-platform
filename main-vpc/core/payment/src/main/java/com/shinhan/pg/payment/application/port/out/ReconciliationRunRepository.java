package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.domain.model.ReconciliationRun;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F08,MVP2-F09] PG·내부 결제·원장 결과의 비교 근거를 남긴다. 자동 금액 보정·운영 종결 이력은 보류다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface ReconciliationRunRepository {
    Optional<ReconciliationRun> findById(UUID id);

    void save(ReconciliationRun value);
}

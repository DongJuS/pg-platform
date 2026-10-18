package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.domain.model.Refund;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F04,MVP2-F05,MVP2-F16] 확정 환불+진행 중 예약액이 승인액을 넘지 않도록 원자적으로 보호한다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface RefundRepository {
    Optional<Refund> findById(UUID id);

    void save(Refund value);
}

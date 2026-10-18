package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F01,MVP2-F02,MVP2-F15] 동기 응답·웹훅을 같은 완료 경로로 수렴시킨다. 승인 불명 중 재승인하지 않는다.
 * TODO: 로컬 DB 스키마·unique·조회 인덱스·반환 부재 계약을 확정한다.
 * 금융 쓰기는 AtomicWritePort 구현의 같은 트랜잭션 안에서 호출한다. 이 save 한 번으로 Outbox 원자성을 주장하지 않는다.
 * </pre>
 */
public interface PaymentRepository {
    Optional<Payment> findById(UUID id);

    void save(Payment value);
}

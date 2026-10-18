package com.shinhan.pg.payment.adapter.out.persistence;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.payment.application.port.out.PaymentRepository;
import com.shinhan.pg.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * <pre>
 * [MVP2-F01,MVP2-F02,MVP2-F15] Payment 영속성 구현 위치.
 * TODO: JPA/JdbcClient/MyBatis 선택 후 connection과 PaymentEntityMapper를 주입한다.
 * TODO: 제약·동시성·원자성·조회 권한을 실제 DB에서 검증한다. 현재 SQL/DB 접근 없음.
 * </pre>
 */
public final class PaymentPersistenceAdapter implements PaymentRepository {
    @Override
    public Optional<Payment> findById(UUID id) {
        throw new FeatureNotImplementedException("MVP2-F01");
    }

    @Override
    public void save(Payment value) {
        throw new FeatureNotImplementedException("MVP2-F01");
    }
}

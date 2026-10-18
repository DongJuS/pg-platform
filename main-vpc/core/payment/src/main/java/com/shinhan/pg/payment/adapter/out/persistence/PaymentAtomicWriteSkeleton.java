package com.shinhan.pg.payment.adapter.out.persistence;

import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.port.out.PaymentAtomicWritePort;

/** 결제 상태와 완료 Outbox를 로컬 DB 트랜잭션으로 기록하고 중복·역순 결과를 처리할 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class PaymentAtomicWriteSkeleton implements PaymentAtomicWritePort {
    @Override
    public void reserveRequest(ApprovePaymentCommand command) {
        throw new UnsupportedOperationException("SKELETON payment: reserveRequest");
    }

    @Override
    public void applyConfirmationOnceWithOutbox(ApplyPaymentConfirmationCommand command) {
        throw new UnsupportedOperationException(
                "SKELETON payment: applyConfirmationOnceWithOutbox");
    }
}

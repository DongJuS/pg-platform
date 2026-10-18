package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;
import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;

/** 결제 상태와 완료 Outbox를 로컬 DB 트랜잭션으로 기록하고 중복·역순 결과를 처리할 위치. */
public interface PaymentAtomicWritePort {
    void reserveRequest(ApprovePaymentCommand command);

    void applyConfirmationOnceWithOutbox(ApplyPaymentConfirmationCommand command);
}

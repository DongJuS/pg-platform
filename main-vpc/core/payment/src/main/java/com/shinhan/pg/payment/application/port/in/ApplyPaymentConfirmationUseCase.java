package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;

/** 검증된 PG 결과 이벤트를 반영한다. 구체 topic/웹훅 변환 계약은 후속이다. */
public interface ApplyPaymentConfirmationUseCase {
    void applyPaymentConfirmation(ApplyPaymentConfirmationCommand command);
}

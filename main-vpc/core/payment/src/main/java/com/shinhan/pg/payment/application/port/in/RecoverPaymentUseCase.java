package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.RecoverPaymentCommand;

public interface RecoverPaymentUseCase {
    void recoverPayment(RecoverPaymentCommand command);
}

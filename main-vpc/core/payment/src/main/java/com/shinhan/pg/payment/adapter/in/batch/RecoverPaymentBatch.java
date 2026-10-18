package com.shinhan.pg.payment.adapter.in.batch;

import com.shinhan.pg.payment.application.command.RecoverPaymentCommand;
import com.shinhan.pg.payment.application.port.in.RecoverPaymentUseCase;

/** TODO: 중단 작업을 조회·재개할 배치 자리. 주기/범위·자동 재시도 한도는 미정이며 @Scheduled는 등록하지 않는다. */
public final class RecoverPaymentBatch {
    private final RecoverPaymentUseCase useCase;

    public RecoverPaymentBatch(RecoverPaymentUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void resume(RecoverPaymentCommand command) {
        useCase.recoverPayment(command);
    }
}

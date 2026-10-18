package com.shinhan.pg.payment.adapter.out.ledger;

import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;
import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;

import java.util.UUID;

/** 확정된 PG 결과에 대한 원장 반영 위치. 동일 업무 ID를 재사용한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class PaymentLedgerRestSkeleton implements LedgerPostingPort {
    @Override
    public UUID postApproval(ApplyPaymentConfirmationCommand command) {
        throw new UnsupportedOperationException("SKELETON payment: postApproval");
    }
}

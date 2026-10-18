package com.shinhan.pg.payment.adapter.out.fep;

import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.port.out.FepPaymentPort;

import java.util.UUID;

/** 금융 대외 요청은 내부 REST → FEP. 반환 ID는 접수 식별자이며 최종 승인과 구분한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class FepPaymentRestSkeleton implements FepPaymentPort {
    @Override
    public UUID requestApproval(ApprovePaymentCommand command) {
        throw new UnsupportedOperationException("SKELETON payment: requestApproval");
    }
}

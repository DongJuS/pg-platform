package com.shinhan.pg.payment;

import com.shinhan.pg.common.archrules.SkeletonWiringAssertions;
import com.shinhan.pg.payment.application.port.in.ApplyPaymentConfirmationUseCase;
import com.shinhan.pg.payment.application.port.in.ApprovePaymentUseCase;
import com.shinhan.pg.payment.application.port.in.CancelPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.GetPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.GetReconciliationUseCase;
import com.shinhan.pg.payment.application.port.in.ListRefundsUseCase;
import com.shinhan.pg.payment.application.port.in.ReceivePgWebhookUseCase;
import com.shinhan.pg.payment.application.port.in.RecoverPaymentUseCase;
import com.shinhan.pg.payment.application.port.in.RequestRefundUseCase;
import com.shinhan.pg.payment.application.port.in.StartReconciliationUseCase;
import com.shinhan.pg.payment.config.PaymentSkeletonConfiguration;

import org.junit.jupiter.api.Test;

class SkeletonWiringTest {
    @Test
    void defaultProfileKeepsSkeletonInactive() {
        SkeletonWiringAssertions.inactive(
                PaymentSkeletonConfiguration.class,
                ApprovePaymentUseCase.class,
                ApplyPaymentConfirmationUseCase.class,
                GetPaymentUseCase.class,
                CancelPaymentUseCase.class,
                RequestRefundUseCase.class,
                ListRefundsUseCase.class,
                ReceivePgWebhookUseCase.class,
                StartReconciliationUseCase.class,
                GetReconciliationUseCase.class,
                RecoverPaymentUseCase.class);
    }

    @Test
    void explicitProfileAssemblesAllDeclaredFlows() {
        SkeletonWiringAssertions.assemblesWithInfrastructure(
                PaymentSkeletonConfiguration.class,
                ApprovePaymentUseCase.class,
                ApplyPaymentConfirmationUseCase.class,
                GetPaymentUseCase.class,
                CancelPaymentUseCase.class,
                RequestRefundUseCase.class,
                ListRefundsUseCase.class,
                ReceivePgWebhookUseCase.class,
                StartReconciliationUseCase.class,
                GetReconciliationUseCase.class,
                RecoverPaymentUseCase.class);
    }
}

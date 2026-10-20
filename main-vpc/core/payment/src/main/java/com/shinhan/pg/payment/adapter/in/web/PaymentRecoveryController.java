package com.shinhan.pg.payment.adapter.in.web;

import com.shinhan.pg.payment.application.command.RecoverPaymentCommand;
import com.shinhan.pg.payment.application.port.in.RecoverPaymentUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * F14: query-only recovery. No scheduled task and no repeated financial POST. Disabled unless
 * configured.
 */
@RestController
@Profile("skeleton")
public final class PaymentRecoveryController {
    private final RecoverPaymentUseCase useCase;

    public PaymentRecoveryController(RecoverPaymentUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/internal/v1/payments/operations/{id}/recover")
    public void recover(@PathVariable UUID id) {
        useCase.recoverPayment(new RecoverPaymentCommand(UUID.randomUUID(), id));
    }
}

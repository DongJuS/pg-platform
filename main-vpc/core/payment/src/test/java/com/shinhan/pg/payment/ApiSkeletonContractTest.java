package com.shinhan.pg.payment;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shinhan.pg.payment.adapter.in.web.PaymentController;
import com.shinhan.pg.payment.adapter.in.web.PaymentExceptionHandler;
import com.shinhan.pg.payment.adapter.in.web.PaymentWebhookController;
import com.shinhan.pg.payment.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.payment.adapter.out.webhook.PgWebhookDecoderAdapter;
import com.shinhan.pg.payment.application.port.out.FepPaymentPort;
import com.shinhan.pg.payment.application.port.out.LedgerPostingPort;
import com.shinhan.pg.payment.application.port.out.PaymentStore;
import com.shinhan.pg.payment.application.service.PaymentApplicationService;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiSkeletonContractTest {
    @Test
    void unauthenticatedPaymentAndInvalidWebhookAreRejectedBeforeEffects() throws Exception {
        var app =
                new PaymentApplicationService(
                        mock(PaymentStore.class),
                        mock(FepPaymentPort.class),
                        mock(LedgerPostingPort.class),
                        (a, b, c, d) -> {},
                        new PgWebhookDecoderAdapter(""),
                        false);
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new PaymentController(app, app, app, app, app),
                                new PaymentWebhookController(app))
                        .setControllerAdvice(
                                new PaymentExceptionHandler(), new PlatformExceptionHandler())
                        .build();
        mvc.perform(
                        post("/api/v1/payments")
                                .contentType("application/json")
                                .header("Idempotency-Key", "key")
                                .content("{\"amount\":30000,\"currency\":\"KRW\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/internal/v1/payments/pg/webhooks")
                                .contentType("application/json")
                                .header("X-PG-Signature", "bad")
                                .requestAttr("callerServiceId", "fep-adapter")
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/payments")
                                .contentType("application/json")
                                .header("Idempotency-Key", "key")
                                .content("{broken"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/mock/v1/pg/webhooks").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
    }
}

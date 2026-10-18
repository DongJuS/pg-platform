package com.shinhan.pg.payment;

import com.shinhan.pg.common.archrules.ApiSkeletonAssertions;
import com.shinhan.pg.payment.adapter.in.web.PaymentController;
import com.shinhan.pg.payment.adapter.in.web.PaymentReconciliationController;
import com.shinhan.pg.payment.adapter.in.web.PaymentWebhookController;
import com.shinhan.pg.payment.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.payment.config.PaymentSkeletonConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class ApiSkeletonContractTest {
    @Test
    void concreteEndpointsStayInactiveByDefaultAndReturn501WhenEnabled() throws Exception {
        ApiSkeletonAssertions.verify(
                PaymentSkeletonConfiguration.class,
                new Class<?>[] {
                    PaymentController.class,
                    PaymentWebhookController.class,
                    PaymentReconciliationController.class
                },
                new PlatformExceptionHandler(),
                List.of(
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F01",
                                "POST",
                                "/api/v1/payments",
                                "{\"orderReference\": \"mock-value\", \"merchantId\":"
                                    + " \"00000000-0000-0000-0000-000000000002\", \"amount\": 1000,"
                                    + " \"currency\": \"KRW\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F02",
                                "GET",
                                "/api/v1/payments/00000000-0000-0000-0000-000000000002",
                                null,
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F03",
                                "POST",
                                "/api/v1/payments/00000000-0000-0000-0000-000000000002/cancel",
                                "{\"reason\": \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F04",
                                "POST",
                                "/api/v1/payments/00000000-0000-0000-0000-000000000002/refunds",
                                "{\"amount\": 1000, \"reason\": \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F05",
                                "GET",
                                "/api/v1/payments/00000000-0000-0000-0000-000000000002/refunds",
                                null,
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F06",
                                "POST",
                                "/mock/v1/pg/webhooks",
                                "{\"eventId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"paymentId\": \"00000000-0000-0000-0000-000000000002\","
                                    + " \"providerCode\": \"mock-value\", \"eventType\":"
                                    + " \"mock-value\", \"occurredAt\": \"2026-10-18T00:00:00Z\","
                                    + " \"externalTransactionId\": \"mock-value\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F08",
                                "POST",
                                "/internal/v1/payments/reconciliation-runs",
                                "{\"businessDate\": \"2026-10-18\"}",
                                ""),
                        new ApiSkeletonAssertions.Call(
                                "MVP2-F09",
                                "GET",
                                "/internal/v1/payments/reconciliation-runs/00000000-0000-0000-0000-000000000002",
                                null,
                                "")),
                "/api/v1/payments",
                "/api/v1/payments/invalid-id");
    }
}

package com.shinhan.pg.payment.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/** Requested amount is distinct from internally confirmed approval and reserved adjustments. */
public record GetPaymentResult(
        UUID paymentId,
        String orderReference,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal refundedAmount,
        BigDecimal reservedAmount,
        String currency,
        String status) {}

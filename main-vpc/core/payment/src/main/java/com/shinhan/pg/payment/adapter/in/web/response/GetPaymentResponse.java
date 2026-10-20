package com.shinhan.pg.payment.adapter.in.web.response;

import java.math.BigDecimal;
import java.util.UUID;

/** Requested amount is distinct from internally confirmed approval and reserved adjustments. */
public record GetPaymentResponse(
        UUID paymentId,
        String orderReference,
        BigDecimal requestedAmount,
        BigDecimal approvedAmount,
        BigDecimal refundedAmount,
        BigDecimal reservedAmount,
        String currency,
        String status) {}

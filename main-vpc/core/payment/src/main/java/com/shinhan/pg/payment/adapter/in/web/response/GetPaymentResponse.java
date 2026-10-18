package com.shinhan.pg.payment.adapter.in.web.response;

import java.math.BigDecimal;
import java.util.UUID;

/** [MVP2-F02] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetPaymentResponse(
        UUID paymentId,
        String orderReference,
        BigDecimal approvedAmount,
        BigDecimal refundedAmount,
        String currency,
        String status) {}

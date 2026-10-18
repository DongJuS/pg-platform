package com.shinhan.pg.payment.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/** [MVP2-F02] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record GetPaymentResult(
        UUID paymentId,
        String orderReference,
        BigDecimal approvedAmount,
        BigDecimal refundedAmount,
        String currency,
        String status) {}

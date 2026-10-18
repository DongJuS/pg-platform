package com.shinhan.pg.payment.domain.error;

/** 업무 오류 식별자 초안. HTTP 상태·표시 문구는 웹 계층에서 최종 계약한다. */
public enum PaymentErrorCode {
    IDEMPOTENCY_CONFLICT,
    REFUND_LIMIT_EXCEEDED,
    INVALID_STATE,
    PG_RESULT_UNKNOWN,
    INVALID_WEBHOOK
}

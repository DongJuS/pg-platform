package com.shinhan.pg.payment.domain.error;

public enum PaymentErrorCode {
    INVALID_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    IDEMPOTENCY_CONFLICT,
    REFUND_LIMIT_EXCEEDED,
    INVALID_STATE,
    PG_RESULT_UNKNOWN,
    INVALID_WEBHOOK,
    PG_DECLINED,
    LEDGER_PENDING,
    CIRCUIT_OPEN
}

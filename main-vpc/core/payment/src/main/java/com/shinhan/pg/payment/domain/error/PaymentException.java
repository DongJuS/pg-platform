package com.shinhan.pg.payment.domain.error;

import java.util.Objects;
import java.util.UUID;

/** Stable error and lookup identifiers; no account, secret or raw financial payload in messages. */
public final class PaymentException extends RuntimeException {
    private final PaymentErrorCode code;
    private final UUID paymentId;
    private final UUID operationId;

    public PaymentException(PaymentErrorCode code) {
        this(code, null, null);
    }

    public PaymentException(PaymentErrorCode code, UUID paymentId, UUID operationId) {
        super(Objects.requireNonNull(code).name());
        this.code = code;
        this.paymentId = paymentId;
        this.operationId = operationId;
    }

    public PaymentErrorCode code() {
        return code;
    }

    public UUID paymentId() {
        return paymentId;
    }

    public UUID operationId() {
        return operationId;
    }
}

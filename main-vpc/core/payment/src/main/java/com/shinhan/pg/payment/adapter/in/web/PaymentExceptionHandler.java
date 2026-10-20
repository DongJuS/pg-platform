package com.shinhan.pg.payment.adapter.in.web;

import com.shinhan.pg.payment.domain.error.PaymentException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class PaymentExceptionHandler {
    public record Error(String code, String message, UUID paymentId, UUID operationId) {}

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Error> handle(PaymentException error) {
        int status =
                switch (error.code()) {
                    case INVALID_REQUEST, INVALID_WEBHOOK -> 400;
                    case UNAUTHORIZED -> 401;
                    case FORBIDDEN -> 403;
                    case NOT_FOUND -> 404;
                    case REFUND_LIMIT_EXCEEDED -> 422;
                    case CIRCUIT_OPEN -> 503;
                    case PG_RESULT_UNKNOWN, LEDGER_PENDING -> 504;
                    case PG_DECLINED -> 502;
                    default -> 409;
                };
        return ResponseEntity.status(status)
                .body(
                        new Error(
                                "PAYMENT-" + error.code(),
                                error.code().name(),
                                error.paymentId(),
                                error.operationId()));
    }
}

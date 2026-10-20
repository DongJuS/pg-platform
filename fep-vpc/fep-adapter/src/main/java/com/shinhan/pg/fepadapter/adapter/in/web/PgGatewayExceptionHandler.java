package com.shinhan.pg.fepadapter.adapter.in.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = FepPgController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class PgGatewayExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class})
    ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest().body(Map.of("code", "FEP-INVALID"));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    ResponseEntity<?> missing() {
        return ResponseEntity.status(404).body(Map.of("code", "FEP-NOT-FOUND"));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> unknown() {
        return ResponseEntity.status(502).body(Map.of("code", "FEP-RESULT-UNKNOWN"));
    }
}

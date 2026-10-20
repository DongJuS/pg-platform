package com.shinhan.pg.mockpg.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = MockPgController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class MockPgExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class})
    ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest().body(Map.of("code", "MOCK-PG-INVALID"));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    ResponseEntity<?> missing() {
        return ResponseEntity.status(404).body(Map.of("code", "MOCK-PG-NOT-FOUND"));
    }
}

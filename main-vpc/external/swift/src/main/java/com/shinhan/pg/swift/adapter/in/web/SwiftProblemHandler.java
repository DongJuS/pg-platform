package com.shinhan.pg.swift.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorResponse;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-100)
public final class SwiftProblemHandler {
    @ExceptionHandler(SwiftProblem.class)
    public ResponseEntity<ErrorResponse> error(SwiftProblem e) {
        int status =
                switch (e.type()) {
                    case INVALID -> 400;
                    case FORBIDDEN -> 403;
                    case UNAUTHORIZED -> 401;
                    case NOT_FOUND -> 404;
                    case CONFLICT -> 409;
                    case EXTERNAL_FAILURE -> 503;
                    default -> 500;
                };
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.code(), e.getMessage(), org.slf4j.MDC.get("traceId")));
    }
}

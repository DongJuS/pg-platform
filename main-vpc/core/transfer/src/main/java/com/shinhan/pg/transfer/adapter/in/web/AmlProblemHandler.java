package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.commonerror.ErrorResponse;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(-10)
public class AmlProblemHandler {
    @ExceptionHandler(AmlProblem.class)
    public ResponseEntity<ErrorResponse> handle(AmlProblem e) {
        int code =
                switch (e.type()) {
                    case INVALID -> 400;
                    case NOT_FOUND -> 404;
                    case CONFLICT -> 409;
                    case UNAUTHORIZED -> 401;
                    case FORBIDDEN -> 403;
                    case EXTERNAL_FAILURE -> 502;
                    case INTERNAL -> 500;
                };
        return ResponseEntity.status(code)
                .body(
                        new ErrorResponse(
                                e.code(),
                                "AML request could not be completed",
                                org.slf4j.MDC.get("traceId")));
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ErrorResponse> storage(org.springframework.dao.DataAccessException e) {
        return ResponseEntity.status(503)
                .body(
                        new ErrorResponse(
                                "AML-STORAGE-UNAVAILABLE",
                                "AML storage unavailable",
                                org.slf4j.MDC.get("traceId")));
    }
}

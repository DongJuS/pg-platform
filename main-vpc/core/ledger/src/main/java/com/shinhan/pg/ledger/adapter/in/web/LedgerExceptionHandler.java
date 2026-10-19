package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorResponse;
import com.shinhan.pg.ledger.domain.error.LedgerException;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 원장 업무 거절(LedgerException)을 LedgerErrorMapper의 상태 계약으로 변환한다.
 * 명시적인 Order로 PlatformExceptionHandler의 catch-all보다 먼저 업무 예외를 처리한다.
 * 업무 오류를 성공 DTO로 바꾸지 않는다.
 */
@RestControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public final class LedgerExceptionHandler {
    private final LedgerErrorMapper errorMapper = new LedgerErrorMapper();

    @ExceptionHandler(LedgerException.class)
    public ResponseEntity<ErrorResponse> ledgerError(LedgerException error) {
        var status = errorMapper.status(error.code());
        var code = "LEDGER-" + error.code().name().replace('_', '-');
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, error.getMessage(), MDC.get("traceId")));
    }
}

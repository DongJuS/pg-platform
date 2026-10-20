package com.shinhan.pg.wealthreport.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorResponse;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Never include raw SQL, client exception body, prompt or credentials in error responses/logs. */
@RestControllerAdvice
@Order(-1)
public final class WealthReportErrorMapper {
    @ExceptionHandler(ReportException.class)
    public ResponseEntity<ErrorResponse> report(ReportException e) {
        return ResponseEntity.status(status(e.code()))
                .cacheControl(CacheControl.noStore())
                .body(
                        new ErrorResponse(
                                "REPORT-" + e.code(),
                                "Report request could not be completed",
                                MDC.get("traceId")));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> database(DataAccessException e) {
        return report(new ReportException(WealthReportErrorCode.PERSISTENCE_UNAVAILABLE));
    }

    public HttpStatus status(WealthReportErrorCode c) {
        return switch (c) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case REPORT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_REQUEST, REPORT_NOT_READY -> HttpStatus.CONFLICT;
            case GENERATION_BUSY -> HttpStatus.TOO_MANY_REQUESTS;
            case SNAPSHOT_INCOMPLETE, AI_RESULT_INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
            case GENERATION_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}

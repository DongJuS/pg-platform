package com.shinhan.pg.auth.adapter.in.web;

import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Auth's locked-OTP response is 423; other application errors retain the common contract. */
@RestControllerAdvice @Order(Ordered.HIGHEST_PRECEDENCE)
public final class AuthExceptionHandler {
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> application(ApplicationException error) {
        var code = error.errorCode();
        var status = code == AuthErrorCode.OTP_LOCKED ? HttpStatus.LOCKED : PlatformExceptionHandler.status(code.type());
        return ResponseEntity.status(status).body(new ErrorResponse(code.code(), code.message(), MDC.get("traceId")));
    }
}

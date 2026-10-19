package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorResponse;
import com.shinhan.pg.transfer.adapter.in.web.response.DuplicateTransferErrorResponse;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * [MVP1-F15~F17,F20] transfer 업무 예외를 HTTP 계약으로 변환한다.
 * 매핑 표는 TransferErrorMapper가 소유한다. 업무 오류를 성공 DTO로 바꾸지 않는다.
 * DUPLICATE_REQUEST(409)는 기존 transferId를 detail(existingTransferId)로 포함한다.
 */
@RestControllerAdvice
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public final class TransferExceptionHandler {
    private final TransferErrorMapper mapper = new TransferErrorMapper();

    @ExceptionHandler(TransferException.class)
    public ResponseEntity<Object> transferError(TransferException error) {
        var status = mapper.status(error.code());
        if (error.code() == TransferErrorCode.DUPLICATE_REQUEST) {
            return ResponseEntity.status(status)
                    .body(
                            new DuplicateTransferErrorResponse(
                                    mapper.code(error.code()),
                                    error.getMessage(),
                                    MDC.get("traceId"),
                                    error.existingTransferId()));
        }
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                mapper.code(error.code()), error.getMessage(), MDC.get("traceId")));
    }
}

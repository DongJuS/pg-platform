package com.shinhan.pg.aigateway.adapter.in.web;

import com.shinhan.pg.aigateway.domain.error.AiPreparationException;
import com.shinhan.pg.aigateway.domain.model.PreparationReason;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** No rejected values, Jackson source fragments, SQL messages, or exception causes in logs/responses. */
@RestControllerAdvice(assignableTypes = AiPreparationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class AiPreparationExceptionHandler {
    public static int status(PreparationReason reason) {
        return switch (reason) {
            case CHECKS_PASSED -> 200;
            case FORBIDDEN_CALLER, RESIDUAL_PII, PROMPT_INJECTION -> 403;
            case INPUT_TOO_LARGE -> 413;
            case UNSUPPORTED_FORMAT -> 415;
            case INSPECTION_UNAVAILABLE, AUDIT_UNAVAILABLE -> 503;
            default -> 400;
        };
    }
    @ExceptionHandler(AiPreparationException.class)
    public ResponseEntity<ErrorResponse> preparation(AiPreparationException error) {
        return error(status(error.reason()), error.reason().name());
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorResponse> invalid(Exception error) { return error(400, "INVALID_INPUT"); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception error) {
        return error(503, "PREPARATION_UNAVAILABLE");
    }
    private ResponseEntity<ErrorResponse> error(int status, String code) {
        return ResponseEntity.status(status).cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new ErrorResponse("AI-" + code, code, MDC.get("traceId")));
    }
}

package com.shinhan.pg.aml.adapter.in.web;

import com.shinhan.pg.aml.domain.error.AmlErrorCode;

import org.springframework.http.HttpStatus;

/** Legacy domain-code mapping; actual wire errors use the safe AmlProblemHandler. */
public final class AmlErrorMapper {
    public HttpStatus status(AmlErrorCode error) {
        return switch (error) {
            case DUPLICATE_EFFECT -> HttpStatus.CONFLICT;
            case INVALID_RULE -> HttpStatus.BAD_REQUEST;
            case REPORT_DELIVERY_PENDING -> HttpStatus.BAD_GATEWAY;
        };
    }
}

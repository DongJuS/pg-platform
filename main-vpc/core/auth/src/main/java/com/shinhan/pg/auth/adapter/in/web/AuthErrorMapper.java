package com.shinhan.pg.auth.adapter.in.web;

import com.shinhan.pg.auth.domain.error.AuthErrorCode;

/**
 * [MVP1-F01..F06] AuthErrorCode → HTTP 계약 매핑. 실제 변환은 공통 PlatformExceptionHandler가
 * ErrorType으로 수행하며 이 매퍼는 상태 계약을 문서화한다. 업무 오류를 성공 DTO로 바꾸지 않는다.
 * OTP 잠금은 AuthExceptionHandler가 423으로 변환한다.
 */
public final class AuthErrorMapper {
    public org.springframework.http.HttpStatus status(AuthErrorCode error) {
        if (error == AuthErrorCode.OTP_LOCKED) return org.springframework.http.HttpStatus.LOCKED;
        return switch (error.type()) {
            case INVALID -> org.springframework.http.HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> org.springframework.http.HttpStatus.NOT_FOUND;
            case CONFLICT -> org.springframework.http.HttpStatus.CONFLICT;
            case UNAUTHORIZED -> org.springframework.http.HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> org.springframework.http.HttpStatus.FORBIDDEN;
            case EXTERNAL_FAILURE -> org.springframework.http.HttpStatus.BAD_GATEWAY;
            case INTERNAL -> org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private AuthErrorMapper() {}
}

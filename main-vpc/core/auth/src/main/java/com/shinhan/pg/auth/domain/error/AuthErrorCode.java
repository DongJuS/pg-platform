package com.shinhan.pg.auth.domain.error;

import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;

/** 업무 오류 식별자. HTTP 상태는 ErrorType에서 공통 매핑되며 표시 문구는 웹 계층 계약이다. */
public enum AuthErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS("AUTH-4091", ErrorType.CONFLICT, "Email already exists"),
    INVALID_CREDENTIALS("AUTH-4010", ErrorType.UNAUTHORIZED, "Invalid credentials"),
    TOKEN_REUSED("AUTH-4092", ErrorType.CONFLICT, "Refresh token reuse detected"),
    OTP_INVALID("AUTH-4011", ErrorType.UNAUTHORIZED, "Invalid OTP"),
    OTP_LOCKED("AUTH-4230", ErrorType.FORBIDDEN, "OTP locked"),
    USER_NOT_FOUND("AUTH-4040", ErrorType.NOT_FOUND, "User not found"),
    TOKEN_INVALID("AUTH-4012", ErrorType.UNAUTHORIZED, "Invalid token");

    private final String code;
    private final ErrorType type;
    private final String message;

    AuthErrorCode(String code, ErrorType type, String message) {
        this.code = code;
        this.type = type;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String message() {
        return message;
    }
}

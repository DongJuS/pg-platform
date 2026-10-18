package com.shinhan.pg.auth.domain.error;

/** 업무 오류 식별자 초안. HTTP 상태·표시 문구는 웹 계층에서 최종 계약한다. */
public enum AuthErrorCode {
    EMAIL_ALREADY_EXISTS,
    INVALID_CREDENTIALS,
    TOKEN_REUSED,
    OTP_INVALID,
    OTP_LOCKED
}

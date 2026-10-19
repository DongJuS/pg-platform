package com.shinhan.pg.account.domain.error;

import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;

/** 업무 오류 식별자 초안. HTTP 상태·표시 문구는 웹 계층에서 최종 계약한다.
 * HTTP 상태는 ErrorType에서 공통 매핑된다(PlatformExceptionHandler). */
public enum AccountErrorCode implements ErrorCode {
    ACCOUNT_NOT_FOUND("ACCOUNT-4040", ErrorType.NOT_FOUND, "Account not found"),
    ACCOUNT_FORBIDDEN("ACCOUNT-4031", ErrorType.FORBIDDEN, "Not the account owner"),
    INVALID_STATUS_TRANSITION("ACCOUNT-4090", ErrorType.CONFLICT, "Status transition not allowed"),
    UNAUTHENTICATED("ACCOUNT-4010", ErrorType.UNAUTHORIZED, "Authenticated user required"),
    INTERNAL_CALLER_REQUIRED("ACCOUNT-4030", ErrorType.FORBIDDEN, "Internal service caller required"),
    INVALID_REQUEST("ACCOUNT-4000", ErrorType.INVALID, "Invalid request"),
    ACCOUNT_NUMBER_GENERATION_FAILED(
            "ACCOUNT-5000", ErrorType.INTERNAL, "Failed to allocate a unique account number");

    private final String code;
    private final ErrorType type;
    private final String message;

    AccountErrorCode(String code, ErrorType type, String message) {
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

package com.shinhan.pg.common.commonerror;
import java.util.Objects;
public final class ApplicationException extends RuntimeException {
    private final ErrorCode errorCode;
    public ApplicationException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode).message());
        this.errorCode = errorCode;
    }
    public ErrorCode errorCode() { return errorCode; }
}

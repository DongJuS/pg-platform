package com.shinhan.pg.transfer.domain;
import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;
// One contract example only; no transfer use case or business decision is implemented.
public enum TransferErrorCode implements ErrorCode {
    INVALID_REQUEST("TRF-001", ErrorType.INVALID, "Invalid transfer request");
    private final String code;
    private final ErrorType type;
    private final String message;
    TransferErrorCode(String code, ErrorType type, String message) { this.code=code; this.type=type; this.message=message; }
    public String code() { return code; }
    public ErrorType type() { return type; }
    public String message() { return message; }
}

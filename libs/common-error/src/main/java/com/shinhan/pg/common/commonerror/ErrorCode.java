package com.shinhan.pg.common.commonerror;
public interface ErrorCode {
    String code();
    ErrorType type();
    String message();
}

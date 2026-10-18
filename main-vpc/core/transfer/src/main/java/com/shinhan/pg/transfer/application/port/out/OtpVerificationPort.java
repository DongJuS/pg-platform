package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;

/** 이체 step-up 인증의 내부 Auth REST 호출 위치. */
public interface OtpVerificationPort {
    void verify(RequestTransferCommand command);
}

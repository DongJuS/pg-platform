package com.shinhan.pg.transfer.adapter.out.auth;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.OtpVerificationPort;

/** 이체 step-up 인증의 내부 Auth REST 호출 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class OtpVerificationRestSkeleton implements OtpVerificationPort {
    @Override
    public void verify(RequestTransferCommand command) {
        throw new UnsupportedOperationException("SKELETON transfer: verify");
    }
}

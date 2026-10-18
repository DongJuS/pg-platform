package com.shinhan.pg.transfer.application.port.in;

import com.shinhan.pg.transfer.application.command.RecoverTransferCommand;

/** UNKNOWN 결과를 원장에서 조회·복구하는 재개 진입점. */
public interface RecoverTransferUseCase {
    void recoverTransfer(RecoverTransferCommand command);
}

package com.shinhan.pg.transfer.application.port.in;

import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;

/** Kafka ledger.posted 반영 또는 REST 결과 확정. transfer.completed 발행 책임은 Transfer에 있다. */
public interface CompleteTransferUseCase {
    void completeTransfer(CompleteTransferCommand command);
}

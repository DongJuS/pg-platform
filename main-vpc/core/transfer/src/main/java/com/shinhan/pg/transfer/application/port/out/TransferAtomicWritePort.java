package com.shinhan.pg.transfer.application.port.out;

import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.readmodel.TransferReservationDraft;

import java.util.UUID;

/**
 * actorUserId+idempotencyKey 중복 요청은 기존 결정대로 충돌 오류. 이체 완료 상태와 transfer.completed Outbox 기록은 같은
 * PostgreSQL 트랜잭션에서 한 번만 반영한다. REST 결과와 ledger.posted가 같은 완료 처리를 사용한다.
 */
public interface TransferAtomicWritePort {
    TransferReservationDraft reserveRequest(RequestTransferCommand command);

    void completeOnceWithOutbox(CompleteTransferCommand command);

    void markUnknown(UUID transferId);
}

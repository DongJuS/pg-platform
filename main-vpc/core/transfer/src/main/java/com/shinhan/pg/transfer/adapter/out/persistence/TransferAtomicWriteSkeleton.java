package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.TransferAtomicWritePort;
import com.shinhan.pg.transfer.application.readmodel.TransferReservationDraft;

import java.util.UUID;

/**
 * actorUserId+idempotencyKey 중복 요청은 기존 결정대로 충돌 오류. 이체 완료 상태와 transfer.completed Outbox 기록은 같은
 * PostgreSQL 트랜잭션에서 한 번만 반영한다. REST 결과와 ledger.posted가 같은 완료 처리를 사용한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에
 * 연결하지 않는다.
 */
public final class TransferAtomicWriteSkeleton implements TransferAtomicWritePort {
    @Override
    public TransferReservationDraft reserveRequest(RequestTransferCommand command) {
        throw new UnsupportedOperationException("SKELETON transfer: reserveRequest");
    }

    @Override
    public void completeOnceWithOutbox(CompleteTransferCommand command) {
        throw new UnsupportedOperationException("SKELETON transfer: completeOnceWithOutbox");
    }

    @Override
    public void markUnknown(UUID transferId) {
        throw new UnsupportedOperationException("SKELETON transfer: markUnknown");
    }
}

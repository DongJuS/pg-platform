package com.shinhan.pg.settlement.adapter.out.persistence;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;
import com.shinhan.pg.settlement.application.port.out.SettlementAtomicWritePort;

/**
 * businessDate/cycle/revision/cutoffAt을 고정하고 단계별 재개·완료 Outbox를 로컬 DB에 기록할 위치. 실제 처리는 미구현이며
 * DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class SettlementAtomicWriteSkeleton implements SettlementAtomicWritePort {
    @Override
    public void recordCheckpoint(CloseSettlementCommand command) {
        throw new UnsupportedOperationException("SKELETON settlement: recordCheckpoint");
    }

    @Override
    public void completeOnceWithOutbox(CloseSettlementCommand command) {
        throw new UnsupportedOperationException("SKELETON settlement: completeOnceWithOutbox");
    }
}

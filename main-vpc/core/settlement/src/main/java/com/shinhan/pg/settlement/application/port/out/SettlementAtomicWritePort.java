package com.shinhan.pg.settlement.application.port.out;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;

/** businessDate/cycle/revision/cutoffAt을 고정하고 단계별 재개·완료 Outbox를 로컬 DB에 기록할 위치. */
public interface SettlementAtomicWritePort {
    void recordCheckpoint(CloseSettlementCommand command);

    void completeOnceWithOutbox(CloseSettlementCommand command);
}

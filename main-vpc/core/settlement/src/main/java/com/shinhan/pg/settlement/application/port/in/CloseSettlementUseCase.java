package com.shinhan.pg.settlement.application.port.in;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;
import com.shinhan.pg.settlement.application.result.CloseSettlementResult;

/** 일 마감 배치 또는 내부 수동 재실행. Kafka 메시지마다 마감하는 기능이 아니다. */
public interface CloseSettlementUseCase {
    CloseSettlementResult closeSettlement(CloseSettlementCommand command);
}

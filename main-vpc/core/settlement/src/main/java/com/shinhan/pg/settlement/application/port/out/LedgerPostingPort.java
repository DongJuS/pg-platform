package com.shinhan.pg.settlement.application.port.out;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;

import java.util.UUID;

/** 대사 후 확정할 정산 분개를 동일 cycle 식별자로 원장에 요청한다. */
public interface LedgerPostingPort {
    UUID postSettlement(CloseSettlementCommand command);
}

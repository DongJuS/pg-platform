package com.shinhan.pg.settlement.adapter.out.ledger;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;
import com.shinhan.pg.settlement.application.port.out.LedgerPostingPort;

import java.util.UUID;

/** 대사 후 확정할 정산 분개를 동일 cycle 식별자로 원장에 요청한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class SettlementLedgerRestSkeleton implements LedgerPostingPort {
    @Override
    public UUID postSettlement(CloseSettlementCommand command) {
        throw new UnsupportedOperationException("SKELETON settlement: postSettlement");
    }
}

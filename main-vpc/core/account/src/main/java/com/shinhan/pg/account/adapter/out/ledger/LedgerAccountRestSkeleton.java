package com.shinhan.pg.account.adapter.out.ledger;

import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.port.out.LedgerAccountPort;

import java.util.UUID;

/**
 * 계좌 개설에 필요한 원장 계정 준비를 내부 REST로 요청한다. 교차 DB 원자성을 가정하지 않는다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class LedgerAccountRestSkeleton implements LedgerAccountPort {
    @Override
    public UUID provision(OpenAccountCommand command) {
        throw new UnsupportedOperationException("SKELETON account: provision");
    }
}

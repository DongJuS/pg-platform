package com.shinhan.pg.settlement.adapter.out.fep;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;
import com.shinhan.pg.settlement.application.port.out.FepSettlementPort;

import java.util.UUID;

/** 동일 정산 파일 식별자로 내부 REST → FEP에 전송한다. 접수와 정산 결과를 구분한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class FepSettlementRestSkeleton implements FepSettlementPort {
    @Override
    public UUID sendFile(CloseSettlementCommand command) {
        throw new UnsupportedOperationException("SKELETON settlement: sendFile");
    }
}

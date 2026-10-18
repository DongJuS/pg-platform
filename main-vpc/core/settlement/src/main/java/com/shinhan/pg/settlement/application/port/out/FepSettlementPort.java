package com.shinhan.pg.settlement.application.port.out;

import com.shinhan.pg.settlement.application.command.CloseSettlementCommand;

import java.util.UUID;

/** 동일 정산 파일 식별자로 내부 REST → FEP에 전송한다. 접수와 정산 결과를 구분한다. */
public interface FepSettlementPort {
    UUID sendFile(CloseSettlementCommand command);
}

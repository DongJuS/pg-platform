package com.shinhan.pg.ledger.application.port.in;

import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;

/** Kafka fep.ack 수신. 확정/역분개는 기존 거래 ID에 대해 중복 없이 처리한다. */
public interface ApplyInstitutionResultUseCase {
    void applyInstitutionResult(ApplyInstitutionResultCommand command);
}

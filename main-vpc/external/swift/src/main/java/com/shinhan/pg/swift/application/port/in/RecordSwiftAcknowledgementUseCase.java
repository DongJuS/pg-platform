package com.shinhan.pg.swift.application.port.in;

import com.shinhan.pg.swift.application.command.RecordSwiftAcknowledgementCommand;

/** FEP REST/pacs.002 결과 판정 후 fep.ack 의도를 기록할 내부 유스케이스. */
public interface RecordSwiftAcknowledgementUseCase {
    void recordSwiftAcknowledgement(RecordSwiftAcknowledgementCommand command);
}

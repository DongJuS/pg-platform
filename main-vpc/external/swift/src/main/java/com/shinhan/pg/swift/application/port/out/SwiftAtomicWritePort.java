package com.shinhan.pg.swift.application.port.out;

import com.shinhan.pg.swift.application.command.RecordSwiftAcknowledgementCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;

/** SWIFT 진행 상태와 Figma의 fep.ack 발행 의도를 같은 로컬 트랜잭션에 기록한다. 실제 pacs.002 판정·Outbox 저장은 미구현이다. */
public interface SwiftAtomicWritePort {
    void recordPending(SubmitSwiftCommand command);

    void recordAcknowledgementWithOutbox(RecordSwiftAcknowledgementCommand command);
}

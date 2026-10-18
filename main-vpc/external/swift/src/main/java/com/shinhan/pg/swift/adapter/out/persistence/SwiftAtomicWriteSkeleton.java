package com.shinhan.pg.swift.adapter.out.persistence;

import com.shinhan.pg.swift.application.command.RecordSwiftAcknowledgementCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.out.SwiftAtomicWritePort;

/**
 * SWIFT 진행 상태와 Figma의 fep.ack 발행 의도를 같은 로컬 트랜잭션에 기록한다. 실제 pacs.002 판정·Outbox 저장은 미구현이다. 실제 처리는
 * 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class SwiftAtomicWriteSkeleton implements SwiftAtomicWritePort {
    @Override
    public void recordPending(SubmitSwiftCommand command) {
        throw new UnsupportedOperationException("SKELETON swift: recordPending");
    }

    @Override
    public void recordAcknowledgementWithOutbox(RecordSwiftAcknowledgementCommand command) {
        throw new UnsupportedOperationException("SKELETON swift: recordAcknowledgementWithOutbox");
    }
}

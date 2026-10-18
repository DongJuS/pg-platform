package com.shinhan.pg.swift.adapter.out.fep;

import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.out.FepSwiftPort;
import com.shinhan.pg.swift.application.readmodel.SwiftAcknowledgementDraft;

import java.util.Optional;
import java.util.UUID;

/**
 * FEP REST 전송·재전송 위치. 같은 송금은 transferId/UETR을 유지하며 새 요청으로 중복 송금하지 않는다. 실제 처리는 미구현이며 DB·HTTP·Kafka에
 * 연결하지 않는다.
 */
public final class FepSwiftRestSkeleton implements FepSwiftPort {
    @Override
    public Optional<SwiftAcknowledgementDraft> findAcknowledgement(String uetr) {
        throw new UnsupportedOperationException("SKELETON swift: findAcknowledgement");
    }

    @Override
    public UUID send(SubmitSwiftCommand command) {
        throw new UnsupportedOperationException("SKELETON swift: send");
    }

    @Override
    public UUID retry(RetrySwiftCommand command) {
        throw new UnsupportedOperationException("SKELETON swift: retry");
    }
}

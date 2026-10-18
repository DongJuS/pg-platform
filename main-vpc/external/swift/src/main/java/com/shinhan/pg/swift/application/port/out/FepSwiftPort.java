package com.shinhan.pg.swift.application.port.out;

import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.readmodel.SwiftAcknowledgementDraft;

import java.util.Optional;
import java.util.UUID;

/** FEP REST 전송·재전송 위치. 같은 송금은 transferId/UETR을 유지하며 새 요청으로 중복 송금하지 않는다. */
public interface FepSwiftPort {
    Optional<SwiftAcknowledgementDraft> findAcknowledgement(String uetr);

    UUID send(SubmitSwiftCommand command);

    UUID retry(RetrySwiftCommand command);
}

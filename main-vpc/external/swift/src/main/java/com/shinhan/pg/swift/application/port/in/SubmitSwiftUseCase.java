package com.shinhan.pg.swift.application.port.in;

import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.result.SubmitSwiftResult;

/** Kafka transfer.outbound 수신 → FEP REST 전송. 주 진입점은 HTTP가 아니다. */
public interface SubmitSwiftUseCase {
    SubmitSwiftResult submitSwift(SubmitSwiftCommand command);
}

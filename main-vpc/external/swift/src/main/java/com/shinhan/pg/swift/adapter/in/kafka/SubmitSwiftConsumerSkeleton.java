package com.shinhan.pg.swift.adapter.in.kafka;

import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.application.result.SubmitSwiftResult;

/**
 * <pre>
 * [MVP5-F03] Kafka transfer.outbound 수신 → FEP REST 전송. 주 진입점은 HTTP가 아니다.
 * TODO: MT103 필드 매핑, UETR 부여
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class SubmitSwiftConsumerSkeleton {
    private final SubmitSwiftUseCase useCase;

    public SubmitSwiftConsumerSkeleton(SubmitSwiftUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public SubmitSwiftResult handle(SubmitSwiftCommand command) {
        return useCase.submitSwift(command);
    }
}

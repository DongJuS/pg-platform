package com.shinhan.pg.swift.adapter.in.kafka;

import com.shinhan.pg.swift.application.command.SubmitSwiftCommand;
import com.shinhan.pg.swift.application.port.in.SubmitSwiftUseCase;
import com.shinhan.pg.swift.application.result.SubmitSwiftResult;

/**
 *
 *
 * <pre>
 * [MVP5-F03] Kafka transfer.outbound 수신 → FEP REST 전송. 주 진입점은 HTTP가 아니다.
 * Stored Transfer UETR and frozen instruction bytes are reused across deliveries.
 * Listener acknowledges after durable submission processing. Operating poison-event/DLQ policy remains deferred.
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

    @org.springframework.kafka.annotation.KafkaListener(
            containerFactory = "swiftKafkaFactory",
            topics = com.shinhan.pg.common.eventcontracts.EventTopics.TRANSFER_OUTBOUND,
            autoStartup = "${pg.swift.enabled:false}",
            properties = "auto.offset.reset=earliest")
    public void outbound(byte[] payload, org.springframework.kafka.support.Acknowledgment ack) {
        var e =
                com.shinhan.pg.common.eventcontracts.EventCodec.fromBytes(
                        payload, com.shinhan.pg.common.eventcontracts.TransferOutboundEvent.class);
        handle(new SubmitSwiftCommand(e.eventId(), e.transferId(), e.uetr()));
        ack.acknowledge();
    }
}

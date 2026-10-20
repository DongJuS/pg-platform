package com.shinhan.pg.ledger.adapter.in.kafka;

import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;

/**
 *
 *
 * <pre>
 * [MVP5-F06,MVP5-F08] Kafka fep.ack 수신. 확정/역분개는 기존 거래 ID에 대해 중복 없이 처리한다.
 * TODO: ACSC → 확정 분개, RJCT → 역분개로 고객 계좌 전액 반환
 * TODO: fep.ack 발행자·소비자와 최종 상태 갱신 책임을 정함. 수신 확인·진행 중·최종 결과를 구분하고 확정·역분개는 멱등 처리.
 * The actual fep.ack listener commits the idempotent financial effect before acknowledging.
 * </pre>
 */
public final class ApplyInstitutionResultConsumerSkeleton {
    private final ApplyInstitutionResultUseCase useCase;

    public ApplyInstitutionResultConsumerSkeleton(ApplyInstitutionResultUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(ApplyInstitutionResultCommand command) {
        useCase.applyInstitutionResult(command);
    }

    @org.springframework.kafka.annotation.KafkaListener(
            containerFactory = "swiftKafkaFactory",
            topics = com.shinhan.pg.common.eventcontracts.EventTopics.FEP_ACK,
            autoStartup = "${pg.swift.enabled:false}",
            properties = "auto.offset.reset=earliest")
    public void onAcknowledgement(
            byte[] payload, org.springframework.kafka.support.Acknowledgment ack) {
        var event =
                com.shinhan.pg.common.eventcontracts.EventCodec.fromBytes(
                        payload, com.shinhan.pg.common.eventcontracts.SwiftAcknowledgedEvent.class);
        handle(
                new ApplyInstitutionResultCommand(
                        event.eventId(), event.transferId(), event.uetr(), event.outcome()));
        ack.acknowledge();
    }
}

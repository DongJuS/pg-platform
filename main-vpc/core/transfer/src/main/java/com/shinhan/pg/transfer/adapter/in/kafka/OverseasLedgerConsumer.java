package com.shinhan.pg.transfer.adapter.in.kafka;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.EventTopics;
import com.shinhan.pg.common.eventcontracts.OverseasLedgerAppliedEvent;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferStore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class OverseasLedgerConsumer {
    private final OverseasTransferStore store;

    public OverseasLedgerConsumer(OverseasTransferStore store) {
        this.store = store;
    }

    @KafkaListener(
            containerFactory = "swiftKafkaFactory",
            topics = EventTopics.OVERSEAS_LEDGER_APPLIED,
            properties = "auto.offset.reset=earliest")
    public void applied(byte[] payload, Acknowledgment ack) {
        store.applied(EventCodec.fromBytes(payload, OverseasLedgerAppliedEvent.class));
        ack.acknowledge();
    }
}

package com.shinhan.pg.aml.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.port.in.EvaluateTransactionUseCase;
import com.shinhan.pg.aml.application.result.EvaluateTransactionResult;
import com.shinhan.pg.common.eventcontracts.LedgerPostedEvent;
import com.shinhan.pg.common.eventcontracts.TransferCompletedEvent;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

/**
 * eventId deduplicates delivery; canonical Ledger transaction identifies the financial fact. No
 * fep.ack inference.
 */
public final class EvaluateTransactionConsumerSkeleton {
    private final EvaluateTransactionUseCase useCase;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public EvaluateTransactionConsumerSkeleton(EvaluateTransactionUseCase useCase) {
        this.useCase = useCase;
    }

    public EvaluateTransactionResult handle(EvaluateTransactionCommand command) {
        return useCase.evaluateTransaction(command);
    }

    @KafkaListener(
            topics = "ledger.posted",
            groupId = "${pg.aml.consumer-group:aml-v1}",
            containerFactory = "amlKafkaFactory",
            autoStartup = "${pg.aml.events-enabled:false}")
    public void ledger(byte[] payload, Acknowledgment ack) throws Exception {
        var e = json.readValue(payload, LedgerPostedEvent.class);
        handle(
                new EvaluateTransactionCommand(
                        UUID.randomUUID(),
                        e.eventId(),
                        e.transactionId(),
                        "ledger.posted",
                        e.journalId()));
        ack.acknowledge();
    }

    @KafkaListener(
            topics = "transfer.completed",
            groupId = "${pg.aml.consumer-group:aml-v1}",
            containerFactory = "amlKafkaFactory",
            autoStartup = "${pg.aml.events-enabled:false}")
    public void transfer(byte[] payload, Acknowledgment ack) throws Exception {
        var e = json.readValue(payload, TransferCompletedEvent.class);
        handle(
                new EvaluateTransactionCommand(
                        UUID.randomUUID(),
                        e.eventId(),
                        e.transactionId(),
                        "transfer.completed",
                        e.journalId()));
        ack.acknowledge();
    }
}

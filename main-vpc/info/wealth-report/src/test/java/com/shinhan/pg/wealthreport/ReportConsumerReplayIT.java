package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.LedgerPostedEvent;
import com.shinhan.pg.common.eventcontracts.TransferCompletedEvent;
import com.shinhan.pg.wealthreport.adapter.in.kafka.UpdateReportSnapshotConsumerSkeleton;
import com.shinhan.pg.wealthreport.config.ReportKafkaConfiguration;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Reproduces fresh-group startup AFTER both durable business events were already published. */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class ReportConsumerReplayIT {
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.0.1"));

    @Test
    void lateFreshConsumerReceivesBothTopicsForOneFinancialEffect() throws Exception {
        try (var admin =
                AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(
                            List.of(
                                    new NewTopic("ledger.posted", 1, (short) 1),
                                    new NewTopic("transfer.completed", 1, (short) 1)))
                    .all()
                    .get(10, TimeUnit.SECONDS);
        }
        UUID transaction = UUID.randomUUID(), journal = UUID.randomUUID();
        try (var producer =
                new KafkaProducer<String, byte[]>(
                        Map.of("bootstrap.servers", KAFKA.getBootstrapServers()),
                        new StringSerializer(),
                        new ByteArraySerializer())) {
            producer.send(
                            new ProducerRecord<>(
                                    "ledger.posted",
                                    transaction.toString(),
                                    EventCodec.toBytes(
                                            new LedgerPostedEvent(
                                                    UUID.randomUUID(),
                                                    transaction,
                                                    journal,
                                                    Instant.now()))))
                    .get(10, TimeUnit.SECONDS);
            producer.send(
                            new ProducerRecord<>(
                                    "transfer.completed",
                                    transaction.toString(),
                                    EventCodec.toBytes(
                                            new TransferCompletedEvent(
                                                    UUID.randomUUID(),
                                                    UUID.randomUUID(),
                                                    transaction,
                                                    journal,
                                                    Instant.now()))))
                    .get(10, TimeUnit.SECONDS);
        }
        var properties = new KafkaProperties();
        properties.setBootstrapServers(List.of(KAFKA.getBootstrapServers()));
        var factory =
                new ReportKafkaConfiguration()
                        .reportKafkaFactory(
                                properties,
                                new StaticListableBeanFactory().getBeanProvider(SslBundles.class));
        var received =
                new java.util.concurrent.CopyOnWriteArrayList<
                        com.shinhan.pg.wealthreport.application.command
                                .UpdateReportSnapshotCommand>();
        var latch = new CountDownLatch(2);
        var handler =
                new UpdateReportSnapshotConsumerSkeleton(
                        command -> {
                            received.add(command);
                            latch.countDown();
                        });
        var ledger = factory.createContainer("ledger.posted");
        var transfer = factory.createContainer("transfer.completed");
        String group = "late-fresh-" + UUID.randomUUID();
        ledger.getContainerProperties().setGroupId(group);
        transfer.getContainerProperties().setGroupId(group);
        ledger.getContainerProperties()
                .setMessageListener(
                        (AcknowledgingMessageListener<String, byte[]>)
                                (record, ack) -> handler.ledger(record.value(), ack));
        transfer.getContainerProperties()
                .setMessageListener(
                        (AcknowledgingMessageListener<String, byte[]>)
                                (record, ack) -> handler.transfer(record.value(), ack));
        try {
            ledger.start();
            transfer.start();
            assertTrue(
                    latch.await(10, TimeUnit.SECONDS),
                    "A fresh consumer skipped pre-existing durable business events");
            assertEquals(2, received.size());
            assertTrue(received.stream().allMatch(c -> transaction.equals(c.transactionId())));
        } finally {
            ledger.stop();
            transfer.stop();
        }
    }
}

package com.shinhan.pg.swift;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.common.eventcontracts.EventCodec;
import com.shinhan.pg.common.eventcontracts.TransferOutboundEvent;
import com.shinhan.pg.swift.adapter.in.kafka.SubmitSwiftConsumerSkeleton;
import com.shinhan.pg.swift.application.result.SubmitSwiftResult;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Actual module YAML deserializer + broker bytes + actual outbound handler, including a late start.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class SwiftConsumerWireIT {
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.0.1"));

    @Test
    void actualModuleConfigurationConsumesOutboxBytes() throws Exception {
        try (var admin =
                AdminClient.create(Map.of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic("transfer.outbound", 1, (short) 1)))
                    .all()
                    .get(10, TimeUnit.SECONDS);
        }
        UUID transfer = UUID.randomUUID();
        String uetr = UUID.randomUUID().toString();
        try (var producer =
                new KafkaProducer<String, byte[]>(
                        Map.of("bootstrap.servers", KAFKA.getBootstrapServers()),
                        new StringSerializer(),
                        new ByteArraySerializer())) {
            producer.send(
                            new ProducerRecord<>(
                                    "transfer.outbound",
                                    transfer.toString(),
                                    EventCodec.toBytes(
                                            new TransferOutboundEvent(
                                                    UUID.randomUUID(),
                                                    transfer,
                                                    uetr,
                                                    Instant.now()))))
                    .get(10, TimeUnit.SECONDS);
        }
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        var properties = yaml.getObject();
        var config = new HashMap<String, Object>();
        config.put("bootstrap.servers", KAFKA.getBootstrapServers());
        config.put("enable.auto.commit", false);
        config.put("auto.offset.reset", "earliest");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(
                "value.deserializer",
                properties.getProperty(
                        "spring.kafka.consumer.value-deserializer",
                        "org.apache.kafka.common.serialization.StringDeserializer"));
        var kafkaProperties = new org.springframework.boot.autoconfigure.kafka.KafkaProperties();
        kafkaProperties.setBootstrapServers(java.util.List.of(KAFKA.getBootstrapServers()));
        var factory =
                new com.shinhan.pg.swift.config.SwiftKafkaConfiguration()
                        .swiftKafkaFactory(
                                kafkaProperties,
                                new org.springframework.beans.factory.support
                                                .StaticListableBeanFactory()
                                        .getBeanProvider(
                                                org.springframework.boot.ssl.SslBundles.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                "org.apache.kafka.common.serialization.ByteArrayDeserializer",
                properties.getProperty("spring.kafka.consumer.value-deserializer"));
        var latch = new CountDownLatch(1);
        var handler =
                new SubmitSwiftConsumerSkeleton(
                        c -> {
                            if (transfer.equals(c.transferId()) && uetr.equals(c.uetr()))
                                latch.countDown();
                            return new SubmitSwiftResult(c.transferId());
                        });
        var container = factory.createContainer("transfer.outbound");
        container.getContainerProperties().setGroupId("wire-" + UUID.randomUUID());
        container
                .getContainerProperties()
                .setMessageListener(
                        (AcknowledgingMessageListener<String, byte[]>)
                                (record, ack) -> handler.outbound(record.value(), ack));
        try {
            container.start();
            assertTrue(
                    latch.await(10, TimeUnit.SECONDS),
                    "Outbox bytes did not reach the actual SWIFT handler");
        } finally {
            container.stop();
        }
    }
}

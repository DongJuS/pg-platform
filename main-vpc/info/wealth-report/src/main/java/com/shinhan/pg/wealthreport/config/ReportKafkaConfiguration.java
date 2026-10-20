package com.shinhan.pg.wealthreport.config;

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Failed notifications remain uncommitted; no arbitrary DLQ/skip policy has been approved. */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
@EnableKafka
public class ReportKafkaConfiguration {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> reportKafkaFactory(
            KafkaProperties p, ObjectProvider<SslBundles> bundles) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
        var consumerProperties = p.buildConsumerProperties(bundles.getIfAvailable());
        // A new projection group must read retained durable events even when it starts after a
        // producer.
        consumerProperties.putIfAbsent(
                org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest");
        factory.setConsumerFactory(
                new DefaultKafkaConsumerFactory<>(
                        consumerProperties, new StringDeserializer(), new ByteArrayDeserializer()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        var errors =
                new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        errors.setAckAfterHandle(false);
        factory.setCommonErrorHandler(errors);
        return factory;
    }
}

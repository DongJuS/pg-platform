package com.shinhan.pg.swift.config;

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Financial delivery is acknowledged only after durable processing. Transient source/DNS/DB
 * failures must not exhaust the framework default retry count and silently skip a transfer with
 * held funds. Finite poison-event quarantine/operations handling remains a separate operating
 * policy.
 */
@Configuration(proxyBeanMethods = false)
@Profile("skeleton")
public class SwiftKafkaConfiguration {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> swiftKafkaFactory(
            KafkaProperties properties, ObjectProvider<SslBundles> bundles) {
        var config = properties.buildConsumerProperties(bundles.getIfAvailable());
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", false);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, byte[]>();
        factory.setConsumerFactory(
                new DefaultKafkaConsumerFactory<>(
                        config, new StringDeserializer(), new ByteArrayDeserializer()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        var errors =
                new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        errors.setAckAfterHandle(false);
        errors.setCommitRecovered(false);
        factory.setCommonErrorHandler(errors);
        return factory;
    }
}

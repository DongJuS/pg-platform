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
        factory.setConsumerFactory(
                new DefaultKafkaConsumerFactory<>(
                        p.buildConsumerProperties(bundles.getIfAvailable()),
                        new StringDeserializer(),
                        new ByteArrayDeserializer()));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        var errors =
                new DefaultErrorHandler(new FixedBackOff(1000, FixedBackOff.UNLIMITED_ATTEMPTS));
        errors.setAckAfterHandle(false);
        factory.setCommonErrorHandler(errors);
        return factory;
    }
}

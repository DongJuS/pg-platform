package com.shinhan.pg.common.outbox;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration(
        afterName = "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true")
@ConditionalOnBean({OutboxStore.class, KafkaTemplate.class})
@EnableScheduling
@org.springframework.boot.context.properties.EnableConfigurationProperties(
        OutboxRelaySettings.class)
public class OutboxAutoConfiguration {
    @Bean
    OutboxRelay outboxRelay(
            OutboxStore store,
            KafkaTemplate<Object, Object> template,
            OutboxRelaySettings settings) {
        // Keep the event ID in the consumer's idempotency contract. Kafka acks alone do not make
        // the DB+Kafka write atomic.
        OutboxPublisher publisher =
                record ->
                        template.send(record.destination(), record.key(), record.payload())
                                .thenApply(result -> null);
        return new OutboxRelay(store, publisher, settings);
    }
}

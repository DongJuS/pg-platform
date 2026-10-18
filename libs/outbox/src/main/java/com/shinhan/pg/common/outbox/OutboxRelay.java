package com.shinhan.pg.common.outbox;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class OutboxRelay {
    private final OutboxStore store;
    private final OutboxPublisher publisher;
    private final OutboxRelaySettings settings;

    public OutboxRelay(OutboxStore store, OutboxPublisher publisher) {
        this(store, publisher, OutboxRelaySettings.defaults());
    }

    public OutboxRelay(OutboxStore store, OutboxPublisher publisher, OutboxRelaySettings settings) {
        this.store = java.util.Objects.requireNonNull(store);
        this.publisher = java.util.Objects.requireNonNull(publisher);
        this.settings = java.util.Objects.requireNonNull(settings);
    }

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:1000}")
    public void poll() {
        var token = UUID.randomUUID();
        for (var record : store.claim(settings.batchSize(), token, settings.lease())) {
            try {
                publisher
                        .publish(record)
                        .toCompletableFuture()
                        .get(settings.acknowledgementTimeout().toMillis(), TimeUnit.MILLISECONDS);
                store.published(record.id(), token);
            } catch (Exception error) {
                store.retry(record.id(), token);
                if (error instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}

package com.shinhan.pg.common.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** 실제 순차 발행 relay의 조정 지점. 기본값은 기존 동작을 유지하며 성능 합격 수치가 아니다. */
@ConfigurationProperties("outbox")
public record OutboxRelaySettings(
        @DefaultValue("20") int batchSize,
        @DefaultValue("2m") Duration lease,
        @DefaultValue("5s") Duration acknowledgementTimeout) {
    public OutboxRelaySettings {
        if (batchSize < 1
                || lease == null
                || acknowledgementTimeout == null
                || acknowledgementTimeout.toMillis() < 1
                || lease.compareTo(acknowledgementTimeout.multipliedBy(batchSize)) <= 0)
            throw new IllegalArgumentException(
                    "Outbox lease must exceed the worst-case sequential acknowledgement budget");
    }

    public static OutboxRelaySettings defaults() {
        return new OutboxRelaySettings(20, Duration.ofMinutes(2), Duration.ofSeconds(5));
    }
}

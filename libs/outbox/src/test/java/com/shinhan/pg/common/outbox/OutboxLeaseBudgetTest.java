package com.shinhan.pg.common.outbox;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class OutboxLeaseBudgetTest {
    @Test
    void rejectsAClaimThatCouldExpireBeforeItsSequentialBatchFinishes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxRelaySettings(100, Duration.ofSeconds(120), Duration.ofSeconds(5)));
    }

    @Test
    void permitsExplicitTuningWithEnoughLeaseTime() {
        assertDoesNotThrow(
                () -> new OutboxRelaySettings(100, Duration.ofMinutes(10), Duration.ofSeconds(5)));
    }
}

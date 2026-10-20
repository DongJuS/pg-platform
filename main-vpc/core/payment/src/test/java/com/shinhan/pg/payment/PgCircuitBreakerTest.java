package com.shinhan.pg.payment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.payment.domain.policy.PgCircuitBreakerPolicy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

class PgCircuitBreakerTest {
    @Test
    void failuresOpenTheCircuitAndOnlyOneProbeIsAdmittedAfterCooldown() {
        var circuit = new PgCircuitBreakerPolicy(2, Duration.ofSeconds(30));
        var now = Instant.parse("2026-10-20T00:00:00Z");
        assertTrue(circuit.acquire(now));
        circuit.failed(now);
        assertTrue(circuit.acquire(now));
        circuit.failed(now);
        assertFalse(circuit.acquire(now.plusSeconds(29)));
        assertTrue(circuit.acquire(now.plusSeconds(30)));
        assertFalse(circuit.acquire(now.plusSeconds(30)));
        circuit.succeeded();
        assertTrue(circuit.acquire(now.plusSeconds(31)));
    }

    @Test
    void failedHalfOpenProbeStartsANewCooldown() {
        var circuit = new PgCircuitBreakerPolicy(1, Duration.ofSeconds(10));
        var now = Instant.parse("2026-10-20T00:00:00Z");
        circuit.failed(now);
        assertTrue(circuit.acquire(now.plusSeconds(10)));
        circuit.failed(now.plusSeconds(10));
        assertFalse(circuit.acquire(now.plusSeconds(19)));
        assertTrue(circuit.acquire(now.plusSeconds(20)));
    }
}

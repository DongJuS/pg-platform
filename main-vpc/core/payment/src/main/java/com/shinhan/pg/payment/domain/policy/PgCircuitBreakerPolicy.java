package com.shinhan.pg.payment.domain.policy;

import java.time.Duration;
import java.time.Instant;

/** F07: bounded failure threshold and a single half-open probe; no synthetic approval fallback. */
public final class PgCircuitBreakerPolicy {
    private final int threshold;
    private final Duration cooldown;
    private int failures;
    private Instant lastFailure;
    private boolean probe;

    public PgCircuitBreakerPolicy() {
        this(5, Duration.ofSeconds(30));
    }

    public PgCircuitBreakerPolicy(int threshold, Duration cooldown) {
        if (threshold < 1 || cooldown.isNegative() || cooldown.isZero())
            throw new IllegalArgumentException("Invalid breaker settings");
        this.threshold = threshold;
        this.cooldown = cooldown;
    }

    public boolean permitsAttempt(int count, Instant last, Instant now) {
        return count < threshold || last != null && !now.isBefore(last.plus(cooldown));
    }

    public synchronized boolean acquire(Instant now) {
        if (failures < threshold) return true;
        if (probe || !permitsAttempt(failures, lastFailure, now)) return false;
        probe = true;
        return true;
    }

    public synchronized void succeeded() {
        failures = 0;
        lastFailure = null;
        probe = false;
    }

    public synchronized void failed(Instant now) {
        failures++;
        lastFailure = now;
        probe = false;
    }
}

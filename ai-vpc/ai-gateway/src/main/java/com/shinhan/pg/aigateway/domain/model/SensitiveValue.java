package com.shinhan.pg.aigateway.domain.model;

/** Request-local hint; never persist or expose the restoration dictionary. */
public record SensitiveValue(PiiType type, String value) {
    @Override public String toString() { return "SensitiveValue[REDACTED]"; }
}

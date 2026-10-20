package com.shinhan.pg.wealthreport.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable read set; capturedAt identifies the source DB read, asOf is the business cutoff.
 * Completeness means all requested accounts in that committed read, not an event-time watermark.
 */
public record AssetSnapshot(
        UUID id,
        UUID userId,
        Instant from,
        Instant to,
        Instant asOf,
        Instant capturedAt,
        String sourceVersion,
        List<AccountFigures> accounts) {
    public AssetSnapshot {
        accounts = List.copyOf(accounts);
    }

    @Override
    public String toString() {
        return "AssetSnapshot[REDACTED]";
    }
}

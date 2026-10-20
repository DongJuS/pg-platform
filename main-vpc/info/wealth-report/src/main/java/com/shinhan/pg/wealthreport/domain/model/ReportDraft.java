package com.shinhan.pg.wealthreport.domain.model;

import java.time.Instant;

/** A future inference contract. It is deliberately different from MVP3 preparation readiness. */
public record ReportDraft(
        String totalAssets,
        String cashIn,
        String cashOut,
        String currency,
        Instant asOf,
        String calculationVersion,
        String explanation,
        String source) {
    @Override
    public String toString() {
        return "ReportDraft[REDACTED]";
    }
}

package com.shinhan.pg.wealthreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Development budgets enforced by bounded execution; not an operating SLO. Gateway route waits
 * 100s.
 */
@ConfigurationProperties("platform.report.execution")
public record ReportExecutionProperties(
        @DefaultValue("90s") Duration totalTimeout,
        @DefaultValue("45s") Duration llmTimeout,
        @DefaultValue("15s") Duration pdfTimeout,
        @DefaultValue("15s") Duration storageTimeout,
        @DefaultValue("2") int maximumConcurrentGenerations) {
    public ReportExecutionProperties {
        if (totalTimeout == null
                || llmTimeout == null
                || pdfTimeout == null
                || storageTimeout == null
                || totalTimeout.isNegative()
                || totalTimeout.isZero()
                || llmTimeout.isNegative()
                || llmTimeout.isZero()
                || pdfTimeout.isNegative()
                || pdfTimeout.isZero()
                || storageTimeout.isNegative()
                || storageTimeout.isZero()
                || maximumConcurrentGenerations < 1)
            throw new IllegalArgumentException("Report budgets and concurrency must be positive");
        if (totalTimeout.compareTo(Duration.ofSeconds(90)) > 0)
            throw new IllegalArgumentException(
                    "Report total timeout must fit the 100s Gateway budget with 10s headroom");
        if (llmTimeout.plus(pdfTimeout).plus(storageTimeout).compareTo(totalTimeout) >= 0)
            throw new IllegalArgumentException(
                    "Report total timeout must leave time for snapshot, validation and token"
                            + " restoration");
    }
}

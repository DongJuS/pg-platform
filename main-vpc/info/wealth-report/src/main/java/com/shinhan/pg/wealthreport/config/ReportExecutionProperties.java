package com.shinhan.pg.wealthreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** 동기 보고서 구현에서 사용할 개발 기본값. 실행 제한 적용 자체는 후속 구현이다. */
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
        if (llmTimeout.plus(pdfTimeout).plus(storageTimeout).compareTo(totalTimeout) >= 0)
            throw new IllegalArgumentException(
                    "Report total timeout must leave time for snapshot, validation and token"
                            + " restoration");
    }
}

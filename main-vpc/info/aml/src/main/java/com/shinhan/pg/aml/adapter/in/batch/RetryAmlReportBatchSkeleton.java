package com.shinhan.pg.aml.adapter.in.batch;

import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;
import com.shinhan.pg.aml.application.port.in.RetryAmlReportUseCase;

/**
 * MVP6-F05 recovery entry point. Retry performs correlated result lookup, never a blind institution
 * resend. Scheduled recovery is assembled by AmlRecoveryConfiguration only in the enabled
 * development profile.
 */
public final class RetryAmlReportBatchSkeleton {
    private final RetryAmlReportUseCase useCase;

    public RetryAmlReportBatchSkeleton(RetryAmlReportUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(RetryAmlReportCommand command) {
        useCase.retryAmlReport(command);
    }
}

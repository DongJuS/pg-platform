package com.shinhan.pg.swift.adapter.in.batch;

import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;

/**
 *
 *
 * <pre>
 * [MVP5-F05] Scheduled recovery queries unresolved stored instructions; explicit operator retry reuses the UETR.
 * Retry budgets and minimum interval are enforced in both Swift/FEP DB claims.
 * No automatic financial retransmission is activated.
 * </pre>
 */
public final class RetrySwiftBatchSkeleton {
    private final RetrySwiftUseCase useCase;
    private final Runnable recovery;

    public RetrySwiftBatchSkeleton(RetrySwiftUseCase useCase) {
        this(useCase, () -> {});
    }

    public RetrySwiftBatchSkeleton(RetrySwiftUseCase useCase, Runnable recovery) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
        this.recovery = java.util.Objects.requireNonNull(recovery);
    }

    public void handle(RetrySwiftCommand command) {
        useCase.retrySwift(command);
    }

    /**
     * Scheduled recovery queries outcomes. Financial retransmission remains an explicit operator
     * action.
     */
    @org.springframework.scheduling.annotation.Scheduled(
            fixedDelayString = "${pg.swift.recovery-interval-ms:10000}")
    public void recover() {
        recovery.run();
    }
}

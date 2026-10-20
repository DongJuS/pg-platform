package com.shinhan.pg.wealthreport.adapter.in.batch;

import com.shinhan.pg.wealthreport.application.command.RecoverReportGenerationCommand;
import com.shinhan.pg.wealthreport.application.port.in.RecoverReportGenerationUseCase;

/** TODO: 기한이 지난 중단 작업을 표시한다. 재호출·재과금·자동 재시도는 수행하지 않는다. */
public final class RecoverReportGenerationBatch {
    private final RecoverReportGenerationUseCase useCase;

    public RecoverReportGenerationBatch(RecoverReportGenerationUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    @org.springframework.scheduling.annotation.Scheduled(
            initialDelayString = "${platform.report.recovery.scan-ms:30000}",
            fixedDelayString = "${platform.report.recovery.scan-ms:30000}")
    public void detectExpired() {
        useCase.recoverReportGeneration(
                new RecoverReportGenerationCommand(java.util.UUID.randomUUID(), null));
    }

    public void resume(RecoverReportGenerationCommand command) {
        useCase.recoverReportGeneration(command);
    }
}

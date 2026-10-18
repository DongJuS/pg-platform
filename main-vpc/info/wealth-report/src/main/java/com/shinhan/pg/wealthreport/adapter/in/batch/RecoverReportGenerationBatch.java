package com.shinhan.pg.wealthreport.adapter.in.batch;

import com.shinhan.pg.wealthreport.application.command.RecoverReportGenerationCommand;
import com.shinhan.pg.wealthreport.application.port.in.RecoverReportGenerationUseCase;

/** TODO: 중단 작업을 조회·재개할 배치 자리. 주기/범위·자동 재시도 한도는 미정이며 @Scheduled는 등록하지 않는다. */
public final class RecoverReportGenerationBatch {
    private final RecoverReportGenerationUseCase useCase;

    public RecoverReportGenerationBatch(RecoverReportGenerationUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void resume(RecoverReportGenerationCommand command) {
        useCase.recoverReportGeneration(command);
    }
}

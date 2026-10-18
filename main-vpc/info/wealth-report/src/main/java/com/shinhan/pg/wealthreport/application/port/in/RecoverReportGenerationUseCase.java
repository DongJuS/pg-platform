package com.shinhan.pg.wealthreport.application.port.in;

import com.shinhan.pg.wealthreport.application.command.RecoverReportGenerationCommand;

public interface RecoverReportGenerationUseCase {
    void recoverReportGeneration(RecoverReportGenerationCommand command);
}

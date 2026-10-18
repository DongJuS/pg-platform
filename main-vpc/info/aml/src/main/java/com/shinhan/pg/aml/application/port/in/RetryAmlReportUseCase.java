package com.shinhan.pg.aml.application.port.in;

import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;

/** 확정 보고 전송 실패를 동일 reportKey로 재시도한다. */
public interface RetryAmlReportUseCase {
    void retryAmlReport(RetryAmlReportCommand command);
}

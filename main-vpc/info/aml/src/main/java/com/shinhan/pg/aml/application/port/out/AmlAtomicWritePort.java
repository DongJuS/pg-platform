package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;

/** 거래 효과 중복 방지·케이스·보고 의도·재시도 상태를 로컬 DB에 기록할 위치. */
public interface AmlAtomicWritePort {
    void recordCaseAndReportIntent(EvaluateTransactionCommand command);

    void recordReportAttempt(RetryAmlReportCommand command);
}

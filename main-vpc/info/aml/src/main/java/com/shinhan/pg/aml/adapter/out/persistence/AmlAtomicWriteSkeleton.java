package com.shinhan.pg.aml.adapter.out.persistence;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;
import com.shinhan.pg.aml.application.port.out.AmlAtomicWritePort;

/** 거래 효과 중복 방지·케이스·보고 의도·재시도 상태를 로컬 DB에 기록할 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class AmlAtomicWriteSkeleton implements AmlAtomicWritePort {
    @Override
    public void recordCaseAndReportIntent(EvaluateTransactionCommand command) {
        throw new UnsupportedOperationException("SKELETON aml: recordCaseAndReportIntent");
    }

    @Override
    public void recordReportAttempt(RetryAmlReportCommand command) {
        throw new UnsupportedOperationException("SKELETON aml: recordReportAttempt");
    }
}

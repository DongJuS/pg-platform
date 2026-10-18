package com.shinhan.pg.aml.adapter.in.batch;

import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;
import com.shinhan.pg.aml.application.port.in.RetryAmlReportUseCase;

/**
 * <pre>
 * [MVP6-F05] 확정 보고 전송 실패를 동일 reportKey로 재시도한다.
 * TODO: 동일 거래의 여러 이벤트를 중복 집계하지 않음. 보고 ID로 재전송을 식별하고 전송 실패·확인 대기·보고 완료·정상 종결을 구분.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
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

package com.shinhan.pg.swift.adapter.in.batch;

import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.port.in.RetrySwiftUseCase;

/**
 * <pre>
 * [MVP5-F05] 무응답 송금은 같은 UETR로 FEP REST 재전송한다.
 * TODO: 정해진 간격·최대 횟수, UETR 고정, fep_db retries 기록
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class RetrySwiftBatchSkeleton {
    private final RetrySwiftUseCase useCase;

    public RetrySwiftBatchSkeleton(RetrySwiftUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(RetrySwiftCommand command) {
        useCase.retrySwift(command);
    }
}

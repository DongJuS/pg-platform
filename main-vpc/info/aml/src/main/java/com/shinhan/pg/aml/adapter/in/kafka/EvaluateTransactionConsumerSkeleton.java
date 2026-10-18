package com.shinhan.pg.aml.adapter.in.kafka;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.port.in.EvaluateTransactionUseCase;
import com.shinhan.pg.aml.application.result.EvaluateTransactionResult;

/**
 * <pre>
 * [MVP6-F01,MVP6-F04] Kafka ledger.posted·transfer.completed 기반 룰 평가와 자동 보고. 거래 효과 중복을 구분한다.
 * TODO: CTR(1천만 원 이상), 분할 거래, 단기 다수 이체 → 위험 점수·케이스
 * TODO: 거래 이벤트 → 규칙 평가 → 위험 점수·케이스 → 보고 대상 자동 판정 → 보고서 생성·전송. GET 조회는 상태를 변경하지 않음.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class EvaluateTransactionConsumerSkeleton {
    private final EvaluateTransactionUseCase useCase;

    public EvaluateTransactionConsumerSkeleton(EvaluateTransactionUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public EvaluateTransactionResult handle(EvaluateTransactionCommand command) {
        return useCase.evaluateTransaction(command);
    }
}

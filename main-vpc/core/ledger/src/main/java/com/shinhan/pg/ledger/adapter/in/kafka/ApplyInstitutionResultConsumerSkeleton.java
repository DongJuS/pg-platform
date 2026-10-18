package com.shinhan.pg.ledger.adapter.in.kafka;

import com.shinhan.pg.ledger.application.command.ApplyInstitutionResultCommand;
import com.shinhan.pg.ledger.application.port.in.ApplyInstitutionResultUseCase;

/**
 * <pre>
 * [MVP5-F06,MVP5-F08] Kafka fep.ack 수신. 확정/역분개는 기존 거래 ID에 대해 중복 없이 처리한다.
 * TODO: ACSC → 확정 분개, RJCT → 역분개로 고객 계좌 전액 반환
 * TODO: fep.ack 발행자·소비자와 최종 상태 갱신 책임을 정함. 수신 확인·진행 중·최종 결과를 구분하고 확정·역분개는 멱등 처리.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class ApplyInstitutionResultConsumerSkeleton {
    private final ApplyInstitutionResultUseCase useCase;

    public ApplyInstitutionResultConsumerSkeleton(ApplyInstitutionResultUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(ApplyInstitutionResultCommand command) {
        useCase.applyInstitutionResult(command);
    }
}

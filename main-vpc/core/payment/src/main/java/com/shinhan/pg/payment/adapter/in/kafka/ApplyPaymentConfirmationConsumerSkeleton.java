package com.shinhan.pg.payment.adapter.in.kafka;

import com.shinhan.pg.payment.application.command.ApplyPaymentConfirmationCommand;
import com.shinhan.pg.payment.application.port.in.ApplyPaymentConfirmationUseCase;

/**
 * <pre>
 * [MVP2-F15] 검증된 PG 결과 이벤트를 반영한다. 구체 topic/웹훅 변환 계약은 후속이다.
 * TODO: 웹훅을 검증·기록한 뒤 상태전이와 원장 반영을 조정. 외부 승인 후 내부 반영 실패는 복구 대상으로 보존.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class ApplyPaymentConfirmationConsumerSkeleton {
    private final ApplyPaymentConfirmationUseCase useCase;

    public ApplyPaymentConfirmationConsumerSkeleton(ApplyPaymentConfirmationUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(ApplyPaymentConfirmationCommand command) {
        useCase.applyPaymentConfirmation(command);
    }
}

package com.shinhan.pg.transfer.adapter.in.kafka;

import com.shinhan.pg.transfer.application.command.CompleteTransferCommand;
import com.shinhan.pg.transfer.application.port.in.CompleteTransferUseCase;

/**
 * <pre>
 * [MVP1-F15,MVP1-F20] Kafka ledger.posted 반영 또는 REST 결과 확정. transfer.completed 발행 책임은 Transfer에 있다.
 * TODO: 중복 이체 요청은 오류로 알림. 잔액 조건 검사·분개·잔액 반영은 Ledger에서 원자 처리. 결과 불명은 기존 transactionId로 확인.
 * TODO: 같은 키 재요청은 중복 오류로 알리고 새 이체를 만들지 않음. 복구 작업은 기존 transactionId로 원장 결과 확인. 조회 404만으로 최종 실패를 단정하지 않음.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class CompleteTransferConsumerSkeleton {
    private final CompleteTransferUseCase useCase;

    public CompleteTransferConsumerSkeleton(CompleteTransferUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(CompleteTransferCommand command) {
        useCase.completeTransfer(command);
    }
}

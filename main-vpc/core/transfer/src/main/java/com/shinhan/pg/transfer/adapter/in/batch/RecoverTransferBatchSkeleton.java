package com.shinhan.pg.transfer.adapter.in.batch;

import com.shinhan.pg.transfer.application.command.RecoverTransferCommand;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;

/**
 * <pre>
 * [MVP1-F20] UNKNOWN 결과를 원장에서 조회·복구하는 재개 진입점.
 * TODO: 같은 키 재요청은 중복 오류로 알리고 새 이체를 만들지 않음. 복구 작업은 기존 transactionId로 원장 결과 확인. 조회 404만으로 최종 실패를 단정하지 않음.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class RecoverTransferBatchSkeleton {
    private final RecoverTransferUseCase useCase;

    public RecoverTransferBatchSkeleton(RecoverTransferUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(RecoverTransferCommand command) {
        useCase.recoverTransfer(command);
    }
}

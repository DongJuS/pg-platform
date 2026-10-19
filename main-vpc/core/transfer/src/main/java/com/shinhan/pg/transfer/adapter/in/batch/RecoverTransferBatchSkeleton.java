package com.shinhan.pg.transfer.adapter.in.batch;

import com.shinhan.pg.transfer.application.command.RecoverTransferCommand;
import com.shinhan.pg.transfer.application.port.in.RecoverTransferUseCase;
import com.shinhan.pg.transfer.application.port.out.TransferQueryPort;

import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;

/**
 * <pre>
 * [MVP1-F20] UNKNOWN 결과를 원장에서 조회·복구하는 재개 진입점.
 * 복구 작업은 기존 transactionId로 원장 결과 확인. 조회 404만으로 최종 실패를 단정하지 않음.
 * </pre>
 *
 * <p>WO-A6 실구현: UNKNOWN 이체를 주기적으로 스캔해 건별 recoverTransfer를 호출한다.
 * 재출금은 없으며 동일 transactionId의 조회만 수행한다. 복구 간격은 미정 정책이라
 * 프로퍼티({@code pg.transfer.recover.interval})로 열고 보수적 기본값 60s만 둔다(지시서 WO-A6).
 * 배치 크기는 {@code pg.transfer.recover.batch-size}(기본 50).
 * 스케줄링 활성화는 TransferSchedulingConfiguration(skeleton 프로필)이 담당한다.</p>
 */
public final class RecoverTransferBatchSkeleton {
    private final RecoverTransferUseCase useCase;
    private final TransferQueryPort transferQueryPort;
    private final int batchSize;

    public RecoverTransferBatchSkeleton(
            RecoverTransferUseCase useCase, TransferQueryPort transferQueryPort, int batchSize) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
        this.transferQueryPort = java.util.Objects.requireNonNull(transferQueryPort);
        this.batchSize = batchSize;
    }

    /** 기존 진입점(직접 command 주입). */
    public void handle(RecoverTransferCommand command) {
        useCase.recoverTransfer(command);
    }

    @Scheduled(fixedDelayString = "${pg.transfer.recover.interval:60s}")
    public void recoverUnknownTransfers() {
        for (var unknown : transferQueryPort.findUnknown(batchSize)) {
            try {
                useCase.recoverTransfer(
                        new RecoverTransferCommand(
                                UUID.randomUUID(), unknown.transferId(), unknown.transactionId()));
            } catch (Exception error) {
                // 건별 실패가 배치를 중단시키지 않는다. UNKNOWN 유지 — 다음 주기에 재시도.
                LoggerFactory.getLogger(getClass())
                        .warn("recover failed for transfer {} — stays UNKNOWN",
                                unknown.transferId(), error);
            }
        }
    }
}

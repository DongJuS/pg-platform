package com.shinhan.pg.wealthreport.adapter.in.kafka;

import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.in.UpdateReportSnapshotUseCase;

/**
 * <pre>
 * [MVP4-F05,MVP4-F09] Kafka 이체·결제 완료로 스냅샷을 갱신한다. 보고서 생성을 호출하지 않는다.
 * TODO: 이벤트 ID 중복 제거, 일자별 집계
 * TODO: 업무 거래 식별자로 중복 집계를 방지하고 보고서 자료 시점을 고정. 지연 이벤트·재구축의 포함 기준은 Q24에서 검토.
 * TODO: KafkaListener/cron·payload mapping·ack/DLQ는 최종 계약 후 등록한다. HTTP API가 아니다.
 * </pre>
 */
public final class UpdateReportSnapshotConsumerSkeleton {
    private final UpdateReportSnapshotUseCase useCase;

    public UpdateReportSnapshotConsumerSkeleton(UpdateReportSnapshotUseCase useCase) {
        this.useCase = java.util.Objects.requireNonNull(useCase);
    }

    public void handle(UpdateReportSnapshotCommand command) {
        useCase.updateReportSnapshot(command);
    }
}

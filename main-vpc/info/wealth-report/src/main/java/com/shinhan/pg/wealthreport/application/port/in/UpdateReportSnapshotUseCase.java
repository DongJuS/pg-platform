package com.shinhan.pg.wealthreport.application.port.in;

import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;

/** Kafka 이체·결제 완료로 스냅샷을 갱신한다. 보고서 생성을 호출하지 않는다. */
public interface UpdateReportSnapshotUseCase {
    void updateReportSnapshot(UpdateReportSnapshotCommand command);
}

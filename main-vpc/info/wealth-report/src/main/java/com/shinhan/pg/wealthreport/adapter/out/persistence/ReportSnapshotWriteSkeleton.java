package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotWritePort;

/** 소비자 eventId·거래 effect 중복 방지와 스냅샷 반영을 같은 로컬 트랜잭션에서 구현한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class ReportSnapshotWriteSkeleton implements ReportSnapshotWritePort {
    @Override
    public void applyOnce(UpdateReportSnapshotCommand command) {
        throw new UnsupportedOperationException("SKELETON wealth-report: applyOnce");
    }
}

package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;

/** 소비자 eventId·거래 effect 중복 방지와 스냅샷 반영을 같은 로컬 트랜잭션에서 구현한다. */
public interface ReportSnapshotWritePort {
    void applyOnce(UpdateReportSnapshotCommand command);
}

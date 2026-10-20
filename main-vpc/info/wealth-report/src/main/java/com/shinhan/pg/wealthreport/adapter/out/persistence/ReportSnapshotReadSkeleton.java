package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

/** Unregistered legacy placeholder. Active source is the internal Ledger snapshot adapter. */
public final class ReportSnapshotReadSkeleton implements ReportSnapshotPort {
    public AssetSnapshot load(GenerateReportCommand c) {
        throw new IllegalStateException("Inactive legacy adapter");
    }
}

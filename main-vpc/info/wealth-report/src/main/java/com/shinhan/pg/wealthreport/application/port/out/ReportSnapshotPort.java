package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

public interface ReportSnapshotPort {
    AssetSnapshot load(GenerateReportCommand request);
}

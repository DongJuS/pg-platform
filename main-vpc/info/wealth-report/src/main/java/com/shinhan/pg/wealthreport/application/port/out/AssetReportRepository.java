package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.domain.model.AssetReport;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetReportRepository {
    AssetReport claim(GenerateReportCommand c, String fingerprint, int capacity, Duration timeout);

    Optional<AssetReport> findById(UUID id);

    List<AssetReport> list(ListReportsCommand c, int limit);

    void stage(UUID id, String stage);

    void saveSnapshot(UUID reportId, AssetSnapshot snapshot);

    void fail(UUID reportId, String code);

    void recoverExpired(UUID reportId);
}

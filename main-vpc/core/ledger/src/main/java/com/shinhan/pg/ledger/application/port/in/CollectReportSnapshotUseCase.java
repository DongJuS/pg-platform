package com.shinhan.pg.ledger.application.port.in;

import com.shinhan.pg.ledger.application.readmodel.ReportLedgerSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CollectReportSnapshotUseCase {
    ReportLedgerSnapshot collect(
            UUID userId, List<UUID> accountIds, Instant from, Instant to, Instant asOf);
}

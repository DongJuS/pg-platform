package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.application.readmodel.ReportLedgerSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReportLedgerSnapshotPort {
    ReportLedgerSnapshot read(
            UUID userId, List<UUID> accounts, Instant from, Instant to, Instant asOf);
}

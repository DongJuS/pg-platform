package com.shinhan.pg.ledger.application.service;

import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.ledger.application.port.in.CollectReportSnapshotUseCase;
import com.shinhan.pg.ledger.application.port.out.AccountQueryPort;
import com.shinhan.pg.ledger.application.port.out.ReportLedgerSnapshotPort;
import com.shinhan.pg.ledger.application.readmodel.ReportLedgerSnapshot;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Verify ownership using Account REST before opening the local ledger read transaction. Figma 29:2
 * fixes the report source; 19:2 preserves service DB boundaries.
 */
public final class CollectReportSnapshotService implements CollectReportSnapshotUseCase {
    private final AccountQueryPort accounts;
    private final ReportLedgerSnapshotPort snapshots;

    public CollectReportSnapshotService(AccountQueryPort a, ReportLedgerSnapshotPort s) {
        accounts = a;
        snapshots = s;
    }

    public ReportLedgerSnapshot collect(
            UUID user, List<UUID> ids, Instant from, Instant to, Instant asOf) {
        if (user == null
                || ids == null
                || ids.isEmpty()
                || ids.size() > 20
                || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()
                || from == null
                || to == null
                || asOf == null
                || !from.isBefore(to)
                || to.isAfter(asOf)
                || asOf.isAfter(Instant.now())) throw failure(ErrorType.INVALID);
        for (UUID id : ids) {
            var a = accounts.getAccount(id);
            if (!user.equals(a.ownerId())) throw failure(ErrorType.FORBIDDEN);
            if (!"KRW".equals(a.currency())) throw failure(ErrorType.INVALID);
        }
        return snapshots.read(user, ids, from, to, asOf);
    }

    private ApplicationException failure(ErrorType type) {
        return new ApplicationException(
                new ErrorCode() {
                    public String code() {
                        return "LEDGER-REPORT-" + type;
                    }

                    public String message() {
                        return "Report source access rejected";
                    }

                    public ErrorType type() {
                        return type;
                    }
                });
    }
}

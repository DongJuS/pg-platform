package com.shinhan.pg.wealthreport.domain.policy;

import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import java.time.Instant;
import java.util.HashSet;

public final class SnapshotConsistencyPolicy {
    public void validateSnapshot(AssetSnapshot s, Instant requestedAsOf) {
        if (s == null
                || s.id() == null
                || s.userId() == null
                || !requestedAsOf.equals(s.asOf())
                || s.from() == null
                || s.to() == null
                || !s.from().isBefore(s.to())
                || s.to().isAfter(s.asOf())
                || s.capturedAt() == null
                || s.asOf().isAfter(s.capturedAt())
                || s.sourceVersion() == null
                || s.sourceVersion().isBlank()
                || s.accounts().isEmpty()) fail();
        var ids = new HashSet<java.util.UUID>();
        for (var a : s.accounts())
            if (a.accountId() == null
                    || !ids.add(a.accountId())
                    || a.version() < 0
                    || a.balance() == null
                    || a.cashIn() == null
                    || a.cashOut() == null
                    || a.cashIn().signum() < 0
                    || a.cashOut().signum() < 0
                    || !"KRW".equals(a.currency())) fail();
    }

    private void fail() {
        throw new ReportException(WealthReportErrorCode.SNAPSHOT_INCOMPLETE);
    }
}

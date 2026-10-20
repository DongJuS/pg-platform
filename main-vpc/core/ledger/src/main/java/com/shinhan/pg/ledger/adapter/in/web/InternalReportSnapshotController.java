package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.common.commonerror.ApplicationException;
import com.shinhan.pg.common.commonerror.ErrorCode;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.ledger.application.port.in.CollectReportSnapshotUseCase;
import com.shinhan.pg.ledger.application.readmodel.ReportLedgerSnapshot;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * POST /internal/v1/ledger/report-snapshots: authenticated wealth-report only. Input: owner, exact
 * account set, [from,to), asOf. Output: committed balances/flows and read identity. Errors
 * 400/403/502; no foreign DB reads. Proposed report policy remains gated at caller.
 */
@RestController
@Profile("skeleton")
public final class InternalReportSnapshotController {
    private final CollectReportSnapshotUseCase snapshots;

    public InternalReportSnapshotController(CollectReportSnapshotUseCase s) {
        snapshots = s;
    }

    public record Request(
            UUID userId, List<UUID> accountIds, Instant from, Instant to, Instant asOf) {}

    @PostMapping("/internal/v1/ledger/report-snapshots")
    public ReportLedgerSnapshot collect(
            @RequestAttribute("callerServiceId") String caller, @RequestBody Request r) {
        if (!"wealth-report".equals(caller))
            throw new ApplicationException(
                    new ErrorCode() {
                        public String code() {
                            return "LEDGER-REPORT-FORBIDDEN";
                        }

                        public String message() {
                            return "Report source caller forbidden";
                        }

                        public ErrorType type() {
                            return ErrorType.FORBIDDEN;
                        }
                    });
        return snapshots.collect(r.userId(), r.accountIds(), r.from(), r.to(), r.asOf());
    }
}

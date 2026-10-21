package com.shinhan.pg.aml.application.service;

import com.shinhan.pg.aml.application.command.EvaluateTransactionCommand;
import com.shinhan.pg.aml.application.command.ListAmlCasesCommand;
import com.shinhan.pg.aml.application.command.RetryAmlReportCommand;
import com.shinhan.pg.aml.application.command.SubmitAmlReportCommand;
import com.shinhan.pg.aml.application.port.in.EvaluateTransactionUseCase;
import com.shinhan.pg.aml.application.port.in.ListAmlCasesUseCase;
import com.shinhan.pg.aml.application.port.in.RetryAmlReportUseCase;
import com.shinhan.pg.aml.application.port.in.SubmitAmlReportUseCase;
import com.shinhan.pg.aml.application.port.out.AmlAtomicWritePort;
import com.shinhan.pg.aml.application.port.out.AmlSourcePort;
import com.shinhan.pg.aml.application.port.out.FepKofiuPort;
import com.shinhan.pg.aml.application.readmodel.AmlCaseSummary;
import com.shinhan.pg.aml.application.result.EvaluateTransactionResult;
import com.shinhan.pg.aml.application.result.ListAmlCasesResult;
import com.shinhan.pg.aml.application.result.SubmitAmlReportResult;
import com.shinhan.pg.aml.domain.model.AmlCase;
import com.shinhan.pg.aml.domain.model.AmlRuleSet;
import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.UUID;

/** MVP6-F01..F05. Authentication is explicit, HTTP is outside local database transactions. */
public final class AmlApplicationService
        implements EvaluateTransactionUseCase,
                ListAmlCasesUseCase,
                RetryAmlReportUseCase,
                SubmitAmlReportUseCase {
    private final FepKofiuPort fep;
    private final AmlAtomicWritePort store;
    private final AmlSourcePort source;
    private final AmlRuleSet policy;
    private final boolean enabled, dispatch, automaticQuery, retransmit;

    public AmlApplicationService(
            FepKofiuPort fep,
            AmlAtomicWritePort store,
            AmlSourcePort source,
            AmlRuleSet policy,
            boolean enabled,
            boolean dispatch,
            boolean automaticQuery,
            boolean retransmit) {
        this.fep = fep;
        this.store = store;
        this.source = source;
        this.policy = policy;
        this.enabled = enabled;
        this.dispatch = dispatch;
        this.automaticQuery = automaticQuery;
        this.retransmit = retransmit;
    }

    public EvaluateTransactionResult evaluateTransaction(EvaluateTransactionCommand c) {
        requireEnabled("MVP6-F01");
        store.receive(c);
        return new EvaluateTransactionResult(c.transactionId());
    }

    /**
     * Acknowledged Kafka records are recoverable in the Inbox even when original source REST is
     * unavailable.
     */
    public void recover() {
        if (!enabled) return;
        store.purgeExpired();
        for (var command : store.pending(50)) {
            AmlSourcePort.Resolution resolution;
            try {
                resolution = source.resolve(command.transactionId(), command.journalId());
            } catch (AmlProblem e) {
                store.defer(
                        command,
                        e.type() == ErrorType.CONFLICT ? "SOURCE_CONFLICT" : "SOURCE_UNAVAILABLE");
                continue;
            }
            store.resolve(command, resolution.fact(), resolution.excludedReason(), policy);
        }
        if (dispatch) for (var report : store.readyReports(20)) send(report);
        if (automaticQuery) for (var report : store.unresolvedReports(20)) queryDocument(report);
    }

    private void send(AmlReportDocument d) {
        UUID worker = UUID.randomUUID();
        if (!store.claimReport(d.reportId(), worker)) return;
        KofiuResult result = null;
        try {
            result = fep.submit(d);
        } catch (AmlProblem e) {
            if (e.type() != ErrorType.EXTERNAL_FAILURE) {
                store.reportResult(d, worker, null);
                throw e;
            }
        }
        store.reportResult(d, worker, result);
    }

    private void queryDocument(AmlReportDocument d) {
        KofiuResult result = null;
        try {
            result = fep.query(d);
        } catch (AmlProblem e) {
            if (e.type() == ErrorType.NOT_FOUND) {
                // No FEP receipt means the request never passed its durable boundary. The same
                // frozen
                // registration is safe.
                try {
                    result = fep.submit(d);
                } catch (AmlProblem ignored) {
                    if (ignored.type() == ErrorType.CONFLICT)
                        result =
                                new KofiuResult(
                                        d.reportId(),
                                        d.reportKey(),
                                        d.hash(),
                                        null,
                                        "UNKNOWN",
                                        "REVIEW",
                                        0,
                                        0);
                    else if (ignored.type() != ErrorType.EXTERNAL_FAILURE) throw ignored;
                }
            } else if (e.type() == ErrorType.CONFLICT) {
                result =
                        new KofiuResult(
                                d.reportId(),
                                d.reportKey(),
                                d.hash(),
                                null,
                                "UNKNOWN",
                                "REVIEW",
                                0,
                                0);
            } else if (e.type() != ErrorType.EXTERNAL_FAILURE) throw e;
        }
        store.reportResult(d, null, result);
    }

    public ListAmlCasesResult listAmlCases(ListAmlCasesCommand c) {
        requireEnabled("MVP6-F02");
        authorize(c.callerServiceId());
        int page = c.page() == null ? 0 : c.page(), size = c.size() == null ? 20 : c.size();
        AmlProblem.require(page >= 0 && page <= 100000 && size >= 1 && size <= 100);
        AmlProblem.require(
                c.status() == null
                        || java.util.Set.of("NO_REPORT", "DETECTED", "REPORT_REQUIRED", "REVIEW")
                                .contains(c.status()));
        AmlProblem.require(c.from() == null || c.to() == null || c.from().isBefore(c.to()));
        store.auditAccess(new UUID(0, 0), "CASE_LIST", c.callerServiceId());
        var rows = store.list(c.status(), c.from(), c.to(), page, size + 1);
        boolean more = rows.size() > size;
        return new ListAmlCasesResult(
                rows.stream()
                        .limit(size)
                        .map(
                                r ->
                                        new AmlCaseSummary(
                                                r.id(),
                                                r.status(),
                                                r.riskScore(),
                                                r.ruleVersion(),
                                                r.createdAt()))
                        .toList(),
                more ? page + 1 : null);
    }

    public AmlCase getCase(String caller, UUID id) {
        requireEnabled("MVP6-F02");
        authorize(caller);
        store.auditAccess(id, "CASE_READ", caller);
        return store.caseById(id);
    }

    public com.shinhan.pg.aml.application.readmodel.AmlCaseDetails details(String caller, UUID id) {
        requireEnabled("MVP6-F02");
        authorize(caller);
        store.auditAccess(id, "CASE_READ", caller);
        return store.caseDetails(id);
    }

    public AmlReportDocument document(String caller, UUID caseId) {
        requireEnabled("MVP6-F02");
        authorize(caller);
        store.auditAccess(caseId, "REPORT_DOCUMENT_READ", caller);
        return store.reportForCase(caseId);
    }

    public SubmitAmlReportResult submitAmlReport(SubmitAmlReportCommand c) {
        requireEnabled("MVP6-F03");
        authorize(c.callerServiceId());
        if (!dispatch) throw new AmlProblem("AML-DISPATCH-DISABLED", ErrorType.CONFLICT);
        var d = store.reportForCase(c.caseId());
        send(d);
        var r = store.reportState(d.reportId());
        return new SubmitAmlReportResult(d.reportId(), r.institutionStatus(), r.receiptReference());
    }

    public KofiuResult report(String caller, UUID caseId) {
        requireEnabled("MVP6-F02");
        authorize(caller);
        return store.reportState(store.reportForCase(caseId).reportId());
    }

    public KofiuResult query(String caller, UUID caseId) {
        requireEnabled("MVP6-F05");
        authorize(caller);
        var d = store.reportForCase(caseId);
        queryDocument(d);
        return store.reportState(d.reportId());
    }

    public KofiuResult retransmit(String caller, UUID caseId) {
        requireEnabled("MVP6-F05");
        authorize(caller);
        if (!retransmit) throw new AmlProblem("AML-RETRANSMIT-DISABLED", ErrorType.CONFLICT);
        var d = store.reportForCase(caseId);
        var result = fep.retransmit(d);
        store.reportResult(d, null, result);
        return store.reportState(d.reportId());
    }

    public KofiuResult amend(String caller, UUID caseId, UUID previousReportId) {
        requireEnabled("MVP6-F03");
        authorize(caller);
        var d = store.amend(caseId, previousReportId);
        if (dispatch) send(d);
        return store.reportState(d.reportId());
    }

    public void reevaluate(String caller) {
        requireEnabled("MVP6-F04");
        authorize(caller);
        if (policy == null) throw new AmlProblem("AML-POLICY-PENDING", ErrorType.CONFLICT);
        store.reevaluate(policy);
    }

    /** Batch retry means result lookup, never a blind institution resend. */
    public void retryAmlReport(RetryAmlReportCommand c) {
        requireEnabled("MVP6-F05");
        AmlProblem.require(c.reportId() != null && c.reportKey() != null);
        var document = store.reportDocument(c.reportId());
        if (!document.reportKey().equals(c.reportKey())) throw AmlProblem.conflict();
        queryDocument(document);
    }

    private void requireEnabled(String feature) {
        if (!enabled) throw new FeatureNotImplementedException(feature);
    }

    private static void authorize(String caller) {
        if (!"aml-operations".equals(caller))
            throw new AmlProblem("AML-ADMIN-REQUIRED", ErrorType.FORBIDDEN);
    }
}

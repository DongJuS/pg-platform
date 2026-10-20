package com.shinhan.pg.wealthreport.application.service;

import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.GetReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.application.command.RecoverReportGenerationCommand;
import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.in.DownloadReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GenerateReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GetReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.ListReportsUseCase;
import com.shinhan.pg.wealthreport.application.port.in.RecoverReportGenerationUseCase;
import com.shinhan.pg.wealthreport.application.port.in.UpdateReportSnapshotUseCase;
import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.application.port.out.AssetReportRepository;
import com.shinhan.pg.wealthreport.application.port.out.GenerationExecutionPort;
import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotWritePort;
import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;
import com.shinhan.pg.wealthreport.application.readmodel.ReportSummary;
import com.shinhan.pg.wealthreport.application.result.GenerateReportResult;
import com.shinhan.pg.wealthreport.application.result.GetReportResult;
import com.shinhan.pg.wealthreport.application.result.ListReportsResult;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AccountFigures;
import com.shinhan.pg.wealthreport.domain.model.AssetReport;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;
import com.shinhan.pg.wealthreport.domain.policy.FinancialMetricsCalculator;
import com.shinhan.pg.wealthreport.domain.policy.ReportNumberValidationPolicy;
import com.shinhan.pg.wealthreport.domain.policy.SnapshotConsistencyPolicy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * MVP4-F01/02/03/05/07/08/09/10. Figma 29:2, ADR 0001 4-A. Auth -> reserve -> collect committed
 * source -> freeze -> calculate -> explanation -> exact validation -> render -> private
 * store+complete. HTTP 201 only after completion. Failures retain stage/cutoff. No token
 * dictionary, model replay, automatic charging or event-triggered generation.
 */
public final class WealthReportApplicationService
        implements GenerateReportUseCase,
                GetReportUseCase,
                ListReportsUseCase,
                UpdateReportSnapshotUseCase,
                RecoverReportGenerationUseCase,
                DownloadReportUseCase {
    private final ReportSnapshotPort snapshots;
    private final AiReportDraftPort ai;
    private final PdfRenderingPort pdf;
    private final ReportStoragePort files;
    private final AssetReportRepository reports;
    private final ReportSnapshotWritePort events;
    private final GenerationExecutionPort execution;
    private final Duration total, llm, pdfBudget, storage;
    private final int capacity;
    private final boolean policyConfirmed;

    public WealthReportApplicationService(
            ReportSnapshotPort snapshots,
            AiReportDraftPort ai,
            PdfRenderingPort pdf,
            ReportStoragePort files,
            AssetReportRepository reports,
            ReportSnapshotWritePort events,
            GenerationExecutionPort execution,
            Duration total,
            Duration llm,
            Duration pdfBudget,
            Duration storage,
            int capacity,
            boolean policyConfirmed) {
        this.snapshots = snapshots;
        this.ai = ai;
        this.pdf = pdf;
        this.files = files;
        this.reports = reports;
        this.events = events;
        this.execution = execution;
        this.total = total;
        this.llm = llm;
        this.pdfBudget = pdfBudget;
        this.storage = storage;
        this.capacity = capacity;
        this.policyConfirmed = policyConfirmed;
    }

    public GenerateReportResult generateReport(GenerateReportCommand input) {
        long deadline = System.nanoTime() + total.toNanos();
        var c = validate(input);
        if (!policyConfirmed) throw fail(WealthReportErrorCode.POLICY_UNCONFIRMED);
        AssetReport r;
        try {
            r = reports.claim(c, fingerprint(c), capacity, total);
        } catch (ReportException e) {
            throw e;
        } catch (RuntimeException e) {
            throw fail(WealthReportErrorCode.PERSISTENCE_UNAVAILABLE);
        }
        if ("COMPLETED".equals(r.status())) return result(r);
        UUID id = r.id();
        String stage = "COLLECTING";
        try {
            reports.stage(id, stage);
            var s = run(() -> snapshots.load(c), Duration.ofSeconds(15), deadline);
            new SnapshotConsistencyPolicy().validateSnapshot(s, c.asOf());
            if (!c.actorUserId().equals(s.userId())
                    || !c.from().equals(s.from())
                    || !c.to().equals(s.to())
                    || !new HashSet<>(c.accountIds())
                            .equals(
                                    new HashSet<>(
                                            s.accounts().stream()
                                                    .map(AccountFigures::accountId)
                                                    .toList())))
                throw fail(WealthReportErrorCode.SNAPSHOT_INCOMPLETE);
            // Validate before numeric SQL storage: never let a numeric column silently round an
            // unapproved fraction.
            stage = "CALCULATING";
            reports.stage(id, stage);
            var metrics = new FinancialMetricsCalculator().calculate(s);
            reports.saveSnapshot(id, s);
            stage = "DRAFTING";
            reports.stage(id, stage);
            var draft = run(() -> ai.requestDraft(metrics), llm, deadline);
            stage = "VALIDATING";
            reports.stage(id, stage);
            new ReportNumberValidationPolicy().validate(metrics, draft);
            stage = "RENDERING";
            reports.stage(id, stage);
            String document = renderText(c, s, metrics, draft);
            var bytes = run(() -> pdf.render(document), pdfBudget, deadline);
            stage = "STORING";
            reports.stage(id, stage);
            run(() -> files.complete(id, bytes, draft.source()), storage, deadline);
            return result(reports.findById(id).orElseThrow());
        } catch (RuntimeException error) {
            var code =
                    error instanceof ReportException e
                            ? e.code()
                            : ("RENDERING".equals(stage)
                                    ? WealthReportErrorCode.PDF_FAILED
                                    : "STORING".equals(stage)
                                            ? WealthReportErrorCode.STORAGE_FAILED
                                            : ("COLLECTING".equals(stage)
                                                            || "DRAFTING".equals(stage))
                                                    ? WealthReportErrorCode.SOURCE_UNAVAILABLE
                                                    : WealthReportErrorCode
                                                            .PERSISTENCE_UNAVAILABLE);
            try {
                reports.fail(id, code.name());
            } catch (RuntimeException ignored) {
                throw fail(WealthReportErrorCode.PERSISTENCE_UNAVAILABLE);
            }
            throw fail(code);
        }
    }

    private <T> T run(Callable<T> fn, Duration step, long deadline) {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw fail(WealthReportErrorCode.GENERATION_TIMEOUT);
        return execution.run(fn, Duration.ofNanos(Math.min(remaining, step.toNanos())));
    }

    private GenerateReportCommand validate(GenerateReportCommand c) {
        if (c == null || c.actorUserId() == null) throw fail(WealthReportErrorCode.UNAUTHENTICATED);
        if (c.from() == null
                || c.to() == null
                || c.asOf() == null
                || !c.from().isBefore(c.to())
                || c.to().isAfter(c.asOf())
                || c.asOf().isAfter(Instant.now())
                || Duration.between(c.from(), c.to()).compareTo(Duration.ofDays(366)) > 0
                || c.accountIds() == null
                || c.accountIds().isEmpty()
                || c.accountIds().size() > 20
                || c.accountIds().stream().anyMatch(Objects::isNull)
                || new HashSet<>(c.accountIds()).size() != c.accountIds().size()
                || c.requestKey() == null
                || !c.requestKey().matches("[A-Za-z0-9._:-]{1,128}"))
            throw fail(WealthReportErrorCode.INVALID_INPUT);
        if (!c.from().equals(c.from().truncatedTo(java.time.temporal.ChronoUnit.MICROS))
                || !c.to().equals(c.to().truncatedTo(java.time.temporal.ChronoUnit.MICROS))
                || !c.asOf().equals(c.asOf().truncatedTo(java.time.temporal.ChronoUnit.MICROS)))
            throw fail(WealthReportErrorCode.INVALID_INPUT);
        return new GenerateReportCommand(
                c.requestId(),
                c.actorUserId(),
                c.from(),
                c.to(),
                c.accountIds().stream().sorted().toList(),
                c.asOf(),
                c.requestKey());
    }

    private String fingerprint(GenerateReportCommand c) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            (c.actorUserId()
                                                            + "|"
                                                            + c.from()
                                                            + "|"
                                                            + c.to()
                                                            + "|"
                                                            + c.asOf()
                                                            + "|"
                                                            + c.accountIds()
                                                            + "|"
                                                            + FinancialMetricsCalculator.VERSION)
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing digest");
        }
    }

    private AssetReport own(UUID user, UUID id) {
        if (user == null) throw fail(WealthReportErrorCode.UNAUTHENTICATED);
        if (id == null) throw fail(WealthReportErrorCode.INVALID_INPUT);
        try {
            var r =
                    reports.findById(id)
                            .orElseThrow(() -> fail(WealthReportErrorCode.REPORT_NOT_FOUND));
            if (!user.equals(r.userId())) throw fail(WealthReportErrorCode.REPORT_NOT_FOUND);
            return r;
        } catch (ReportException e) {
            throw e;
        } catch (RuntimeException e) {
            throw fail(WealthReportErrorCode.PERSISTENCE_UNAVAILABLE);
        }
    }

    public GetReportResult getReport(GetReportCommand c) {
        var r = own(c.actorUserId(), c.reportId());
        return new GetReportResult(
                r.id(),
                r.status(),
                r.asOf(),
                "COMPLETED".equals(r.status()) ? url(r.id()) : null,
                null,
                r.stage(),
                r.failureCode(),
                r.draftSource());
    }

    public ListReportsResult listReports(ListReportsCommand c) {
        if (c.actorUserId() == null) throw fail(WealthReportErrorCode.UNAUTHENTICATED);
        int page = c.page() == null ? 0 : c.page(), size = c.size() == null ? 20 : c.size();
        if (page < 0
                || page > 10000
                || size < 1
                || size > 100
                || (c.from() != null && c.to() != null && !c.from().isBefore(c.to()))
                || (c.status() != null
                        && !Set.of("PROCESSING", "COMPLETED", "FAILED", "TIMED_OUT", "INTERRUPTED")
                                .contains(c.status())))
            throw fail(WealthReportErrorCode.INVALID_INPUT);
        try {
            var rs =
                    reports.list(
                            new ListReportsCommand(
                                    c.requestId(),
                                    c.actorUserId(),
                                    c.from(),
                                    c.to(),
                                    c.status(),
                                    page,
                                    size),
                            size + 1);
            return new ListReportsResult(
                    rs.stream()
                            .limit(size)
                            .map(
                                    r ->
                                            new ReportSummary(
                                                    r.id(), r.status(), r.asOf(), r.completedAt()))
                            .toList(),
                    rs.size() > size ? page + 1 : null);
        } catch (RuntimeException e) {
            throw fail(WealthReportErrorCode.PERSISTENCE_UNAVAILABLE);
        }
    }

    public byte[] download(UUID user, UUID id) {
        var r = own(user, id);
        if (!"COMPLETED".equals(r.status())) throw fail(WealthReportErrorCode.REPORT_NOT_READY);
        try {
            return files.read(id);
        } catch (RuntimeException e) {
            throw fail(WealthReportErrorCode.STORAGE_FAILED);
        }
    }

    public void updateReportSnapshot(UpdateReportSnapshotCommand c) {
        events.applyOnce(c);
    }

    public void recoverReportGeneration(RecoverReportGenerationCommand c) {
        reports.recoverExpired(c.resourceId());
    }

    private GenerateReportResult result(AssetReport r) {
        return new GenerateReportResult(
                r.id(), r.status(), url(r.id()), r.completedAt(), r.draftSource());
    }

    private URI url(UUID id) {
        return URI.create("/api/v1/wealth/reports/" + id + "/pdf");
    }

    private ReportException fail(WealthReportErrorCode c) {
        return new ReportException(c);
    }

    private String renderText(
            GenerateReportCommand c, AssetSnapshot s, FinancialMetrics m, ReportDraft d) {
        return "자산관리 보고서\n"
                + ("TEST_DOUBLE".equals(d.source())
                        ? "테스트 대역 설명 - 실제 AI 보고서가 아닙니다\n"
                        : "AI 설명 보고서\n")
                + "기준 시점: "
                + m.asOf()
                + "\n기간: "
                + c.from()
                + " ~ "
                + c.to()
                + " (종료 제외)\n"
                + "수집 시점: "
                + s.capturedAt()
                + "\n계산 버전: "
                + m.calculationVersion()
                + "\n\n"
                + "금융 지표\t금액 (KRW)\n"
                + "선택 예금계좌 잔액 합계\t"
                + m.totalAssets().amount().toPlainString()
                + "\n"
                + "기간 총입금\t"
                + m.cashIn().amount().toPlainString()
                + "\n"
                + "기간 총출금\t"
                + m.cashOut().amount().toPlainString()
                + "\n\n"
                + "대상 계좌 수: "
                + s.accounts().size()
                + "\n계좌 간 이체를 포함한 계좌별 총현금흐름입니다.\n"
                + "투자수익률 및 부채를 포함한 순자산은 계산하지 않습니다.\n\n설명\n"
                + d.explanation();
    }
}

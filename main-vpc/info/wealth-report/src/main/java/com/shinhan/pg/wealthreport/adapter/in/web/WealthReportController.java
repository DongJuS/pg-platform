package com.shinhan.pg.wealthreport.adapter.in.web;

import com.shinhan.pg.wealthreport.adapter.in.web.request.GenerateReportRequest;
import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.GetReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.application.port.in.DownloadReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GenerateReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GetReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.ListReportsUseCase;
import com.shinhan.pg.wealthreport.application.result.GenerateReportResult;
import com.shinhan.pg.wealthreport.application.result.GetReportResult;
import com.shinhan.pg.wealthreport.application.result.ListReportsResult;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * MVP4-F01/02/03. Figma 29:2 + ADR 0001 4-A, Notion API candidates cross-checked. Gateway validates
 * JWT and replaces external identity headers with X-User-Id (existing MVP1 contract). POST returns
 * completed PDF receipt synchronously, GET never restarts generation. Internal service ports must
 * remain private; PDF URL also requires the owner's JWT on every request.
 */
@RestController
@Profile("skeleton")
public final class WealthReportController {
    private final GenerateReportUseCase generate;
    private final GetReportUseCase get;
    private final ListReportsUseCase list;
    private final DownloadReportUseCase download;

    public WealthReportController(
            GenerateReportUseCase g,
            GetReportUseCase r,
            ListReportsUseCase l,
            DownloadReportUseCase d) {
        generate = g;
        get = r;
        list = l;
        download = d;
    }

    /**
     * Input from/to/asOf (UTC microsecond precision), 1..20 owned account IDs; required
     * Idempotency-Key. 201 completed; 400 invalid,401 identity,404 ownership-safe absence,409
     * duplicate/busy, 422 invalid draft/snapshot,503 missing policy/inference/storage,504 timeout.
     */
    @PostMapping("/api/v1/wealth/reports")
    public ResponseEntity<GenerateReportResult> generateReport(
            @RequestHeader(name = "X-User-Id", required = false) UUID user,
            @RequestBody GenerateReportRequest r,
            @RequestHeader("Idempotency-Key") String key) {
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore())
                .body(
                        generate.generateReport(
                                new GenerateReportCommand(
                                        UUID.randomUUID(),
                                        user,
                                        r.from(),
                                        r.to(),
                                        r.accountIds(),
                                        r.asOf(),
                                        key)));
    }

    @GetMapping("/api/v1/wealth/reports/{reportId}")
    public ResponseEntity<GetReportResult> getReport(
            @RequestHeader(name = "X-User-Id", required = false) UUID user,
            @PathVariable UUID reportId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(get.getReport(new GetReportCommand(UUID.randomUUID(), user, reportId)));
    }

    @GetMapping("/api/v1/wealth/reports")
    public ResponseEntity<ListReportsResult> listReports(
            @RequestHeader(name = "X-User-Id", required = false) UUID user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        list.listReports(
                                new ListReportsCommand(
                                        UUID.randomUUID(), user, from, to, status, page, size)));
    }

    /** Same owner check as detail. No S3 public ACL or transferable bearer/presigned link. */
    @GetMapping("/api/v1/wealth/reports/{reportId}/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestHeader(name = "X-User-Id", required = false) UUID user,
            @PathVariable UUID reportId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=wealth-report.pdf")
                .header("X-Content-Type-Options", "nosniff")
                .body(download.download(user, reportId));
    }
}

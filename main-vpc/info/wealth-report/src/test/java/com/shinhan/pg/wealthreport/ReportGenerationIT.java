package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.wealthreport.adapter.out.execution.BoundedGenerationExecutor;
import com.shinhan.pg.wealthreport.adapter.out.pdf.PdfRenderingSkeleton;
import com.shinhan.pg.wealthreport.adapter.out.persistence.AssetReportPersistenceAdapter;
import com.shinhan.pg.wealthreport.adapter.out.persistence.PrivateReportStorageAdapter;
import com.shinhan.pg.wealthreport.adapter.out.persistence.ReportSnapshotWriteSkeleton;
import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.GetReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.application.command.RecoverReportGenerationCommand;
import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.out.AiReportDraftPort;
import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;
import com.shinhan.pg.wealthreport.application.service.WealthReportApplicationService;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AccountFigures;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;
import com.shinhan.pg.wealthreport.domain.model.FinancialMetrics;
import com.shinhan.pg.wealthreport.domain.model.ReportDraft;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * Real PostgreSQL/files and actual renderer. Only explanation and source are explicit test ports.
 */
@Testcontainers
@EnabledIfSystemProperty(named = "integration", matches = "true")
class ReportGenerationIT {
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    DriverManagerDataSource ds;
    JdbcTemplate jdbc;
    AssetReportPersistenceAdapter reports;
    PrivateReportStorageAdapter files;
    ReportSnapshotWriteSkeleton events;
    UUID user, account;
    BoundedGenerationExecutor executor;
    GenerateReportCommand c;
    static final Instant AS_OF = ReportPoliciesTest.AS_OF;

    @BeforeEach
    void setup() {
        ds = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(ds);
        jdbc.execute(
                "truncate"
                    + " asset_reports,report_stages,asset_snapshots,snapshot_accounts,report_files,snapshot_events,snapshot_effects"
                    + " cascade");
        reports = new AssetReportPersistenceAdapter(ds);
        files = new PrivateReportStorageAdapter(ds);
        events = new ReportSnapshotWriteSkeleton(ds);
        executor = new BoundedGenerationExecutor(2);
        user = UUID.randomUUID();
        account = UUID.randomUUID();
        c =
                new GenerateReportCommand(
                        UUID.randomUUID(),
                        user,
                        AS_OF.minusSeconds(86400),
                        AS_OF,
                        List.of(account),
                        AS_OF,
                        "test-key");
    }

    @AfterEach
    void close() {
        executor.close();
    }

    AssetSnapshot source(GenerateReportCommand c) {
        return new AssetSnapshot(
                UUID.randomUUID(),
                c.actorUserId(),
                c.from(),
                c.to(),
                c.asOf(),
                AS_OF,
                "test-source",
                List.of(
                        new AccountFigures(
                                account,
                                new BigDecimal("123456"),
                                new BigDecimal("20000"),
                                new BigDecimal("5000"),
                                "KRW",
                                3)));
    }

    ReportDraft draft(FinancialMetrics m) {
        return new ReportDraft(
                m.totalAssets().amount().toPlainString(),
                m.cashIn().amount().toPlainString(),
                m.cashOut().amount().toPlainString(),
                "KRW",
                m.asOf(),
                m.calculationVersion(),
                "계좌의 흐름을 확인하세요.",
                "TEST_DOUBLE");
    }

    WealthReportApplicationService service(
            ReportSnapshotPort s,
            AiReportDraftPort ai,
            PdfRenderingPort pdf,
            ReportStoragePort f,
            Duration llm) {
        return new WealthReportApplicationService(
                s,
                ai,
                pdf,
                f,
                reports,
                events,
                executor,
                Duration.ofSeconds(10),
                llm,
                Duration.ofSeconds(3),
                Duration.ofSeconds(2),
                2,
                true);
    }

    WealthReportApplicationService normal() {
        return service(
                this::source,
                this::draft,
                new PdfRenderingSkeleton(),
                files,
                Duration.ofSeconds(2));
    }

    long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    UUID reportId() {
        return jdbc.queryForObject("select id from asset_reports", UUID.class);
    }

    @Test
    void completedPdfStoredWithSnapshotAndReplayWithoutRegeneration() throws Exception {
        var s = normal();
        var result = s.generateReport(c);
        assertEquals("COMPLETED", result.status());
        assertEquals("TEST_DOUBLE", result.draftSource());
        byte[] pdf = s.download(user, result.reportId());
        try (var doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("자산관리 보고서"));
            assertTrue(text.contains("123456"));
            assertTrue(text.contains("실제 AI 보고서가 아닙니다"));
        }
        assertEquals(result.reportId(), s.generateReport(c).reportId());
        assertEquals(1, count("asset_snapshots"));
        assertEquals(1, count("report_files"));
        assertEquals(
                "123456",
                jdbc.queryForObject("select balance::text from snapshot_accounts", String.class));
        java.nio.file.Path output = java.nio.file.Path.of("build/evidence/mvp4-synthetic.pdf");
        java.nio.file.Files.createDirectories(output.getParent());
        java.nio.file.Files.write(output, pdf);
    }

    @Test
    void longKoreanParagraphWrapsAcrossPagesWithoutDroppingValues() throws Exception {
        String paragraph = "계좌의 현금흐름을 확인하세요. ".repeat(100);
        var s =
                service(
                        this::source,
                        m ->
                                new ReportDraft(
                                        m.totalAssets().amount().toPlainString(),
                                        m.cashIn().amount().toPlainString(),
                                        m.cashOut().amount().toPlainString(),
                                        "KRW",
                                        m.asOf(),
                                        m.calculationVersion(),
                                        paragraph,
                                        "TEST_DOUBLE"),
                        new PdfRenderingSkeleton(),
                        files,
                        Duration.ofSeconds(2));
        var r = s.generateReport(c);
        byte[] bytes = s.download(user, r.reportId());
        try (var doc = Loader.loadPDF(bytes)) {
            assertTrue(doc.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(doc);
            assertEquals(100, text.replaceAll("\\s", "").split("확인하세요", -1).length - 1);
            assertTrue(text.contains("123456"));
        }
        var path = java.nio.file.Path.of("build/evidence/mvp4-multipage.pdf");
        java.nio.file.Files.createDirectories(path.getParent());
        java.nio.file.Files.write(path, bytes);
    }

    @Test
    void ownerChecksApplyToDetailListAndPdf() {
        var s = normal();
        var r = s.generateReport(c);
        UUID other = UUID.randomUUID();
        assertEquals(
                WealthReportErrorCode.REPORT_NOT_FOUND,
                assertThrows(
                                ReportException.class,
                                () ->
                                        s.getReport(
                                                new GetReportCommand(
                                                        UUID.randomUUID(), other, r.reportId())))
                        .code());
        assertThrows(ReportException.class, () -> s.download(other, r.reportId()));
        assertThrows(ReportException.class, () -> s.download(null, r.reportId()));
        assertTrue(
                s.listReports(
                                new ListReportsCommand(
                                        UUID.randomUUID(), other, null, null, null, null, null))
                        .reports()
                        .isEmpty());
        assertEquals(
                1,
                s.listReports(
                                new ListReportsCommand(
                                        UUID.randomUUID(), user, null, null, null, null, null))
                        .reports()
                        .size());
    }

    @Test
    void badInputsAndPolicyGateDoNotCreateRows() {
        var s = normal();
        assertThrows(
                ReportException.class,
                () ->
                        s.generateReport(
                                new GenerateReportCommand(
                                        c.requestId(),
                                        null,
                                        c.from(),
                                        c.to(),
                                        c.accountIds(),
                                        c.asOf(),
                                        c.requestKey())));
        assertThrows(
                ReportException.class,
                () ->
                        s.generateReport(
                                new GenerateReportCommand(
                                        c.requestId(),
                                        user,
                                        c.to(),
                                        c.from(),
                                        c.accountIds(),
                                        c.asOf(),
                                        c.requestKey())));
        assertThrows(
                ReportException.class,
                () ->
                        s.generateReport(
                                new GenerateReportCommand(
                                        c.requestId(),
                                        user,
                                        c.from(),
                                        c.to(),
                                        List.of(account, account),
                                        c.asOf(),
                                        c.requestKey())));
        var gated =
                new WealthReportApplicationService(
                        this::source,
                        this::draft,
                        new PdfRenderingSkeleton(),
                        files,
                        reports,
                        events,
                        executor,
                        Duration.ofSeconds(10),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(2),
                        2,
                        false);
        assertEquals(
                WealthReportErrorCode.POLICY_UNCONFIRMED,
                assertThrows(ReportException.class, () -> gated.generateReport(c)).code());
        assertEquals(0, count("asset_reports"));
    }

    @Test
    void numericMismatchPersistsFailedStageAndNoPdf() {
        var s =
                service(
                        this::source,
                        m ->
                                new ReportDraft(
                                        "999",
                                        "20000",
                                        "5000",
                                        "KRW",
                                        m.asOf(),
                                        m.calculationVersion(),
                                        "설명",
                                        "TEST_DOUBLE"),
                        new PdfRenderingSkeleton(),
                        files,
                        Duration.ofSeconds(2));
        assertEquals(
                WealthReportErrorCode.AI_RESULT_INVALID,
                assertThrows(ReportException.class, () -> s.generateReport(c)).code());
        var r = reports.findById(reportId()).orElseThrow();
        assertEquals("FAILED", r.status());
        assertEquals("VALIDATING", r.stage());
        assertEquals(0, count("report_files"));
        assertThrows(ReportException.class, () -> normal().generateReport(c));
    }

    @Test
    void timeoutRetainsStageAndDoesNotPublishLateResult() throws Exception {
        var s =
                service(
                        this::source,
                        m -> {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return draft(m);
                        },
                        new PdfRenderingSkeleton(),
                        files,
                        Duration.ofMillis(20));
        assertEquals(
                WealthReportErrorCode.GENERATION_TIMEOUT,
                assertThrows(ReportException.class, () -> s.generateReport(c)).code());
        assertEquals("TIMED_OUT", reports.findById(reportId()).orElseThrow().status());
        assertEquals(0, count("report_files"));
    }

    @Test
    void processInterruptionIsMarkedWithoutReplayingAi() {
        var r = reports.claim(c, "fingerprint", 2, Duration.ofSeconds(10));
        reports.stage(r.id(), "DRAFTING");
        jdbc.update(
                "update asset_reports set deadline=clock_timestamp()-interval '1 second' where"
                        + " id=?",
                r.id());
        normal().recoverReportGeneration(
                        new RecoverReportGenerationCommand(UUID.randomUUID(), null));
        var found = reports.findById(r.id()).orElseThrow();
        assertEquals("INTERRUPTED", found.status());
        assertEquals("DRAFTING", found.stage());
        assertThrows(
                ReportException.class,
                () -> files.complete(r.id(), "%PDF-test".getBytes(), "TEST_DOUBLE"));
        assertEquals(0, count("report_files"));
    }

    @Test
    void distributedClaimsSerializeConcurrencyAndConflictingIdempotency() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 8; i++)
                tasks.add(
                        () -> {
                            try {
                                reports.claim(c, "f", 2, Duration.ofSeconds(10));
                                return true;
                            } catch (ReportException e) {
                                return false;
                            }
                        });
            int accepted = 0;
            for (var f : pool.invokeAll(tasks)) if (f.get()) accepted++;
            assertEquals(1, accepted);
        }
        UUID u2 = UUID.randomUUID();
        var c2 =
                new GenerateReportCommand(
                        c.requestId(), u2, c.from(), c.to(), c.accountIds(), c.asOf(), "two");
        reports.claim(c2, "f2", 2, Duration.ofSeconds(10));
        var c3 =
                new GenerateReportCommand(
                        c.requestId(),
                        UUID.randomUUID(),
                        c.from(),
                        c.to(),
                        c.accountIds(),
                        c.asOf(),
                        "three");
        assertEquals(
                WealthReportErrorCode.GENERATION_BUSY,
                assertThrows(
                                ReportException.class,
                                () -> reports.claim(c3, "f3", 2, Duration.ofSeconds(10)))
                        .code());
        assertEquals(2, count("asset_reports"));
    }

    @Test
    void duplicateReverseAndCrossTopicEventsNeverAccumulateTheSameFinancialEffect() {
        UUID tx = UUID.randomUUID(), journal = UUID.randomUUID(), event = UUID.randomUUID();
        var first =
                new UpdateReportSnapshotCommand("transfer.completed", event, tx, journal, AS_OF);
        events.applyOnce(first);
        events.applyOnce(first);
        events.applyOnce(
                new UpdateReportSnapshotCommand(
                        "ledger.posted", event, tx, journal, AS_OF.minusSeconds(1)));
        events.applyOnce(
                new UpdateReportSnapshotCommand(
                        "ledger.posted", UUID.randomUUID(), tx, journal, AS_OF));
        assertEquals(1, count("snapshot_effects"));
        assertEquals(3, count("snapshot_events"));
        assertEquals(0, count("asset_reports"));
        assertThrows(
                ReportException.class,
                () ->
                        events.applyOnce(
                                new UpdateReportSnapshotCommand(
                                        "ledger.posted",
                                        UUID.randomUUID(),
                                        tx,
                                        UUID.randomUUID(),
                                        AS_OF)));
        assertEquals(3, count("snapshot_events"));
    }

    @Test
    void stagesFailClosedForSourceRendererAndStorage() {
        for (String step : List.of("COLLECTING", "RENDERING", "STORING")) {
            jdbc.execute(
                    "truncate"
                        + " asset_reports,report_stages,asset_snapshots,snapshot_accounts,report_files"
                        + " cascade");
            ReportSnapshotPort src =
                    "COLLECTING".equals(step)
                            ? request -> {
                                throw new IllegalStateException("synthetic-secret");
                            }
                            : this::source;
            PdfRenderingPort renderer =
                    "RENDERING".equals(step)
                            ? text -> {
                                throw new ReportException(WealthReportErrorCode.PDF_FAILED);
                            }
                            : new PdfRenderingSkeleton();
            ReportStoragePort store =
                    "STORING".equals(step)
                            ? new ReportStoragePort() {
                                public java.net.URI complete(UUID id, byte[] pdf, String source) {
                                    throw new ReportException(WealthReportErrorCode.STORAGE_FAILED);
                                }

                                public byte[] read(UUID id) {
                                    throw new IllegalStateException();
                                }
                            }
                            : files;
            var error =
                    assertThrows(
                            ReportException.class,
                            () ->
                                    service(
                                                    src,
                                                    this::draft,
                                                    renderer,
                                                    store,
                                                    Duration.ofSeconds(2))
                                            .generateReport(c));
            assertFalse(error.toString().contains("synthetic-secret"));
            assertEquals("FAILED", reports.findById(reportId()).orElseThrow().status());
            assertEquals(step, reports.findById(reportId()).orElseThrow().stage());
            assertEquals(0, count("report_files"));
        }
    }

    @Test
    void incompleteSnapshotAndStorageTransactionRollback() {
        var s =
                service(
                        request ->
                                new AssetSnapshot(
                                        UUID.randomUUID(),
                                        user,
                                        c.from(),
                                        c.to(),
                                        AS_OF,
                                        AS_OF,
                                        "test",
                                        List.of()),
                        this::draft,
                        new PdfRenderingSkeleton(),
                        files,
                        Duration.ofSeconds(2));
        assertThrows(ReportException.class, () -> s.generateReport(c));
        assertEquals(0, count("asset_snapshots"));
        jdbc.execute(
                "truncate"
                    + " asset_reports,report_stages,asset_snapshots,snapshot_accounts,report_files"
                    + " cascade");
        var r = reports.claim(c, "f", 2, Duration.ofSeconds(10));
        jdbc.execute("alter table report_files add constraint injected_fail check (false)");
        try {
            assertThrows(
                    RuntimeException.class,
                    () -> files.complete(r.id(), "%PDF-test".getBytes(), "TEST_DOUBLE"));
            assertEquals("PROCESSING", reports.findById(r.id()).orElseThrow().status());
            assertEquals(0, count("report_files"));
        } finally {
            jdbc.execute("alter table report_files drop constraint injected_fail");
        }
    }
}

package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * Development storage in the report DB. Atomic publication prevents failed-stage PDFs becoming
 * visible.
 */
public final class PrivateReportStorageAdapter implements ReportStoragePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public PrivateReportStorageAdapter(DataSource ds) {
        jdbc = new JdbcTemplate(ds);
        jdbc.setQueryTimeout(5);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setTimeout(5);
    }

    public URI complete(UUID id, byte[] pdf, String source) {
        if (pdf == null
                || pdf.length < 5
                || pdf.length > 5242880
                || !new String(pdf, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
            throw new ReportException(WealthReportErrorCode.STORAGE_FAILED);
        tx.executeWithoutResult(
                t -> {
                    if (jdbc.update(
                                    "update asset_reports set"
                                        + " status='COMPLETED',stage='COMPLETED',draft_source=?,completed_at=clock_timestamp()"
                                        + " where id=? and status='PROCESSING' and"
                                        + " deadline>clock_timestamp()",
                                    source,
                                    id)
                            != 1)
                        throw new ReportException(WealthReportErrorCode.GENERATION_TIMEOUT);
                    jdbc.update("insert into report_files(report_id,pdf) values(?,?)", id, pdf);
                    jdbc.update(
                            "insert into report_stages(report_id,stage) values(?,'COMPLETED')", id);
                });
        return URI.create("/api/v1/wealth/reports/" + id + "/pdf");
    }

    public byte[] read(UUID id) {
        return jdbc
                .query(
                        "select f.pdf from report_files f join asset_reports r on r.id=f.report_id"
                                + " where f.report_id=? and r.status='COMPLETED'",
                        (r, n) -> r.getBytes(1),
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ReportException(WealthReportErrorCode.REPORT_NOT_READY));
    }
}

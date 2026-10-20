package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.adapter.out.persistence.entity.AssetReportEntity;
import com.shinhan.pg.wealthreport.adapter.out.persistence.mapper.AssetReportEntityMapper;
import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.application.port.out.AssetReportRepository;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.shinhan.pg.wealthreport.domain.model.AssetReport;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;
import com.shinhan.pg.wealthreport.domain.policy.FinancialMetricsCalculator;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * Short local transactions only. PostgreSQL advisory reservation lock bounds all replicas. No
 * HTTP/PDF operation is executed while holding a DB transaction.
 */
public final class AssetReportPersistenceAdapter implements AssetReportRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final RowMapper<AssetReport> row =
            (r, n) ->
                    new AssetReportEntityMapper()
                            .toDomain(
                                    new AssetReportEntity(
                                            r.getObject("id", UUID.class),
                                            r.getObject("user_id", UUID.class),
                                            r.getTimestamp("as_of").toInstant(),
                                            r.getString("status"),
                                            r.getString("stage"),
                                            r.getString("calculation_version"),
                                            r.getString("failure_code"),
                                            r.getString("draft_source"),
                                            r.getTimestamp("completed_at") == null
                                                    ? null
                                                    : r.getTimestamp("completed_at").toInstant()));

    public AssetReportPersistenceAdapter(DataSource ds) {
        jdbc = new JdbcTemplate(ds);
        jdbc.setQueryTimeout(5);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setTimeout(5);
    }

    public AssetReport claim(
            GenerateReportCommand c, String fingerprint, int capacity, Duration timeout) {
        return tx.execute(
                t -> {
                    jdbc.execute("select pg_advisory_xact_lock(740004)");
                    var existing =
                            jdbc.query(
                                    "select * from asset_reports where user_id=? and request_key=?",
                                    row,
                                    c.actorUserId(),
                                    c.requestKey());
                    if (!existing.isEmpty()) {
                        String old =
                                jdbc.queryForObject(
                                        "select fingerprint from asset_reports where id=?",
                                        String.class,
                                        existing.getFirst().id());
                        if (!fingerprint.equals(old)
                                || !"COMPLETED".equals(existing.getFirst().status()))
                            throw error(WealthReportErrorCode.DUPLICATE_REQUEST);
                        return existing.getFirst();
                    }
                    if (jdbc.queryForObject(
                                    "select count(*) from asset_reports where user_id=? and"
                                            + " status='PROCESSING'",
                                    Integer.class,
                                    c.actorUserId())
                            > 0) throw error(WealthReportErrorCode.GENERATION_BUSY);
                    if (jdbc.queryForObject(
                                    "select count(*) from asset_reports where status='PROCESSING'",
                                    Integer.class)
                            >= capacity) throw error(WealthReportErrorCode.GENERATION_BUSY);
                    UUID id = UUID.randomUUID();
                    jdbc.update(
                            "insert into"
                                + " asset_reports(id,user_id,request_key,fingerprint,as_of,status,stage,calculation_version,deadline)"
                                + " values(?,?,?,?,?,'PROCESSING','RESERVED',?,clock_timestamp()+(?"
                                + " * interval '1 millisecond'))",
                            id,
                            c.actorUserId(),
                            c.requestKey(),
                            fingerprint,
                            Timestamp.from(c.asOf()),
                            FinancialMetricsCalculator.VERSION,
                            timeout.toMillis());
                    jdbc.update(
                            "insert into report_stages(report_id,stage) values(?,'RESERVED')", id);
                    return findById(id).orElseThrow();
                });
    }

    public Optional<AssetReport> findById(UUID id) {
        return jdbc.query("select * from asset_reports where id=?", row, id).stream().findFirst();
    }

    public List<AssetReport> list(ListReportsCommand c, int limit) {
        var sql = new StringBuilder("select * from asset_reports where user_id=?");
        var args = new ArrayList<Object>();
        args.add(c.actorUserId());
        if (c.from() != null) {
            sql.append(" and created_at>=?");
            args.add(Timestamp.from(c.from()));
        }
        if (c.to() != null) {
            sql.append(" and created_at<?");
            args.add(Timestamp.from(c.to()));
        }
        if (c.status() != null) {
            sql.append(" and status=?");
            args.add(c.status());
        }
        sql.append(" order by created_at desc,id limit ? offset ?");
        args.add(limit);
        args.add(c.page() * c.size());
        return jdbc.query(sql.toString(), row, args.toArray());
    }

    public void stage(UUID id, String stage) {
        tx.executeWithoutResult(
                t -> {
                    if (jdbc.update(
                                    "update asset_reports set stage=? where id=? and"
                                            + " status='PROCESSING' and deadline>clock_timestamp()",
                                    stage,
                                    id)
                            != 1) throw error(WealthReportErrorCode.GENERATION_TIMEOUT);
                    jdbc.update(
                            "insert into report_stages(report_id,stage) values(?,?) on conflict do"
                                    + " nothing",
                            id,
                            stage);
                });
    }

    public void saveSnapshot(UUID id, AssetSnapshot s) {
        tx.executeWithoutResult(
                t -> {
                    stage(id, "CALCULATING");
                    jdbc.update(
                            "insert into asset_snapshots values(?,?,?,?,?,?,?,?)",
                            s.id(),
                            id,
                            s.userId(),
                            Timestamp.from(s.from()),
                            Timestamp.from(s.to()),
                            Timestamp.from(s.asOf()),
                            Timestamp.from(s.capturedAt()),
                            s.sourceVersion());
                    for (var a : s.accounts())
                        jdbc.update(
                                "insert into snapshot_accounts values(?,?,?,?,?,?,?)",
                                s.id(),
                                a.accountId(),
                                a.balance(),
                                a.cashIn(),
                                a.cashOut(),
                                a.currency(),
                                a.version());
                });
    }

    public void fail(UUID id, String code) {
        String status = "GENERATION_TIMEOUT".equals(code) ? "TIMED_OUT" : "FAILED";
        jdbc.update(
                "update asset_reports set status=?,failure_code=? where id=? and"
                        + " status='PROCESSING'",
                status,
                code,
                id);
    }

    /** Detect interruption only; no model replay, retry, charging or 202 work queue. */
    public void recoverExpired(UUID id) {
        if (id == null)
            jdbc.update(
                    "update asset_reports set status='INTERRUPTED',failure_code='INTERRUPTED' where"
                            + " status='PROCESSING' and deadline<=clock_timestamp()");
        else
            jdbc.update(
                    "update asset_reports set status='INTERRUPTED',failure_code='INTERRUPTED' where"
                            + " id=? and status='PROCESSING' and deadline<=clock_timestamp()",
                    id);
    }

    private ReportException error(WealthReportErrorCode c) {
        return new ReportException(c);
    }
}

package com.shinhan.pg.wealthreport.adapter.out.persistence;

import com.shinhan.pg.wealthreport.application.command.UpdateReportSnapshotCommand;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotWritePort;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;

import javax.sql.DataSource;

/**
 * Durable notification projection, not an amount aggregate. Event and financial effect dedup are
 * separate. Out-of-order and delayed events preserve occurredAt without moving a balance watermark
 * backwards.
 */
public final class ReportSnapshotWriteSkeleton implements ReportSnapshotWritePort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public ReportSnapshotWriteSkeleton(DataSource ds) {
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setTimeout(5);
    }

    public void applyOnce(UpdateReportSnapshotCommand c) {
        if (c == null
                || c.eventId() == null
                || c.transactionId() == null
                || c.journalId() == null
                || c.occurredAt() == null
                || !("ledger.posted".equals(c.topic()) || "transfer.completed".equals(c.topic())))
            throw new ReportException(WealthReportErrorCode.INVALID_INPUT);
        tx.executeWithoutResult(
                t -> {
                    jdbc.update(
                            "insert into snapshot_effects(transaction_id,journal_id,occurred_at)"
                                    + " values(?,?,?) on conflict(transaction_id) do nothing",
                            c.transactionId(),
                            c.journalId(),
                            Timestamp.from(c.occurredAt()));
                    var journal =
                            jdbc.queryForObject(
                                    "select journal_id from snapshot_effects where"
                                            + " transaction_id=?",
                                    java.util.UUID.class,
                                    c.transactionId());
                    if (!c.journalId().equals(journal))
                        throw new ReportException(WealthReportErrorCode.SNAPSHOT_INCOMPLETE);
                    jdbc.update(
                            "insert into snapshot_events values(?,?,?,?,?) on"
                                    + " conflict(topic,event_id) do nothing",
                            c.topic(),
                            c.eventId(),
                            c.transactionId(),
                            c.journalId(),
                            Timestamp.from(c.occurredAt()));
                    var stored =
                            jdbc.queryForMap(
                                    "select transaction_id,journal_id,occurred_at from"
                                            + " snapshot_events where topic=? and event_id=?",
                                    c.topic(),
                                    c.eventId());
                    if (!c.transactionId().equals(stored.get("transaction_id"))
                            || !c.journalId().equals(stored.get("journal_id"))
                            || !c.occurredAt()
                                    .equals(((Timestamp) stored.get("occurred_at")).toInstant()))
                        throw new ReportException(WealthReportErrorCode.SNAPSHOT_INCOMPLETE);
                });
    }
}

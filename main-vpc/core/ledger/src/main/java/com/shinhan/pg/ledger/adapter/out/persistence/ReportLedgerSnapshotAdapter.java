package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.ledger.application.port.out.ReportLedgerSnapshotPort;
import com.shinhan.pg.ledger.application.readmodel.ReportLedgerSnapshot;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * One repeatable-read DB view for all requested accounts. A historical balance subtracts postings
 * after asOf from the balance in that same read (including a test opening balance). No
 * current-balance lookup from a separate HTTP call is mixed into the snapshot. Late commits can
 * change future collections: no event-time completeness/SLA is claimed.
 */
public final class ReportLedgerSnapshotAdapter implements ReportLedgerSnapshotPort {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public ReportLedgerSnapshotAdapter(DataSource ds) {
        jdbc = new JdbcTemplate(ds);
        jdbc.setQueryTimeout(5);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setReadOnly(true);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        tx.setTimeout(5);
    }

    public ReportLedgerSnapshot read(
            UUID user, List<UUID> accounts, Instant from, Instant to, Instant asOf) {
        return tx.execute(
                t -> {
                    // Establish the InnoDB consistent read before querying its timestamp and
                    // remaining rows.
                    jdbc.query("select id from ledger_accounts limit 1", (r, n) -> r.getString(1));
                    long micros =
                            jdbc.queryForObject(
                                    "select unix_timestamp(current_timestamp(6))*1000000",
                                    Long.class);
                    Instant captured =
                            Instant.ofEpochSecond(
                                    Math.floorDiv(micros, 1000000),
                                    Math.floorMod(micros, 1000000) * 1000);
                    if (asOf.isAfter(captured))
                        throw new IllegalArgumentException("Invalid source cutoff");
                    var values = new ArrayList<ReportLedgerSnapshot.AccountFigures>();
                    for (UUID id : accounts) {
                        var current =
                                jdbc.queryForList(
                                        "select id,balance,currency,version from ledger_accounts"
                                                + " where external_account_id=?",
                                        id.toString());
                        if (current.isEmpty()) {
                            values.add(
                                    new ReportLedgerSnapshot.AccountFigures(
                                            id,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO,
                                            BigDecimal.ZERO,
                                            "KRW",
                                            0));
                            continue;
                        }
                        var a = current.getFirst();
                        if (!"KRW".equals(a.get("currency")))
                            throw new IllegalArgumentException("Unsupported source currency");
                        var sums =
                                jdbc.queryForMap(
                                        "select coalesce(sum(case when j.posted_at>? then"
                                            + " p.signed_amount else 0 end),0) as later,"
                                            + " coalesce(sum(case when j.posted_at>=? and"
                                            + " j.posted_at<? and p.signed_amount>0 then"
                                            + " p.signed_amount else 0 end),0) as incoming,"
                                            + " coalesce(sum(case when j.posted_at>=? and"
                                            + " j.posted_at<? and p.signed_amount<0 then"
                                            + " -p.signed_amount else 0 end),0) as outgoing from"
                                            + " postings p join journals j on j.id=p.journal_id"
                                            + " where p.ledger_account_id=?",
                                        Timestamp.from(asOf),
                                        Timestamp.from(from),
                                        Timestamp.from(to),
                                        Timestamp.from(from),
                                        Timestamp.from(to),
                                        a.get("id"));
                        values.add(
                                new ReportLedgerSnapshot.AccountFigures(
                                        id,
                                        ((BigDecimal) a.get("balance"))
                                                .subtract((BigDecimal) sums.get("later")),
                                        (BigDecimal) sums.get("incoming"),
                                        (BigDecimal) sums.get("outgoing"),
                                        "KRW",
                                        ((Number) a.get("version")).longValue()));
                    }
                    UUID readId = UUID.randomUUID();
                    return new ReportLedgerSnapshot(
                            readId,
                            user,
                            from,
                            to,
                            asOf,
                            captured,
                            "ledger-read-" + readId,
                            List.copyOf(values));
                });
    }
}

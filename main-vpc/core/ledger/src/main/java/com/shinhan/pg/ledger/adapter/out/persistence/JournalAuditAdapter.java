package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.ledger.application.port.out.JournalAuditPort;
import com.shinhan.pg.ledger.application.readmodel.JournalAudit;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/** Read-only owning-Ledger export. Joins stay inside Ledger's MariaDB. */
public final class JournalAuditAdapter implements JournalAuditPort {
    private final JdbcTemplate jdbc;

    public JournalAuditAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public JournalAudit read(UUID id) {
        return new JournalAudit(
                id,
                jdbc.query(
                        "select a.external_account_id,p.signed_amount,p.currency from journals j"
                            + " join postings p on p.journal_id=j.id join ledger_accounts a on"
                            + " a.id=p.ledger_account_id where j.transaction_id=? order by"
                            + " a.external_account_id,p.id",
                        (r, i) ->
                                new JournalAudit.Line(
                                        UUID.fromString(r.getString(1)),
                                        r.getBigDecimal(2),
                                        r.getString(3)),
                        id.toString()));
    }
}

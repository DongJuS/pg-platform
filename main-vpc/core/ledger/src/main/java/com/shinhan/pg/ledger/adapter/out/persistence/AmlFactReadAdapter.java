package com.shinhan.pg.ledger.adapter.out.persistence;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.LedgerEvidence;
import com.shinhan.pg.ledger.application.port.out.AmlFactReadPort;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class AmlFactReadAdapter implements AmlFactReadPort {
    private final JdbcTemplate jdbc;

    public AmlFactReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public LedgerEvidence read(UUID id) {
        var js =
                jdbc.queryForList(
                        "select id,posted_at from journals where transaction_id=?", id.toString());
        if (js.isEmpty()) throw AmlProblem.missing();
        var j = js.getFirst();
        String journal = (String) j.get("id");
        var lines =
                jdbc.query(
                        "select a.external_account_id,p.signed_amount,p.currency from postings p"
                                + " join ledger_accounts a on a.id=p.ledger_account_id where"
                                + " p.journal_id=? order by a.external_account_id",
                        (r, n) ->
                                new LedgerEvidence.Line(
                                        UUID.fromString(r.getString(1)),
                                        r.getBigDecimal(2),
                                        r.getString(3)),
                        journal);
        var os =
                jdbc.queryForList(
                        "select transfer_id,hold_journal_id,outcome from overseas_reservations"
                                + " where hold_journal_id=? or final_journal_id=?",
                        journal,
                        journal);
        UUID transfer = null;
        String phase = null, outcome = null;
        if (!os.isEmpty()) {
            var o = os.getFirst();
            transfer = UUID.fromString((String) o.get("transfer_id"));
            phase = journal.equals(o.get("hold_journal_id")) ? "HOLD" : "FINAL";
            outcome = (String) o.get("outcome");
        }
        return new LedgerEvidence(
                id,
                UUID.fromString(journal),
                ((java.sql.Timestamp) j.get("posted_at")).toInstant(),
                lines,
                transfer,
                phase,
                outcome);
    }
}

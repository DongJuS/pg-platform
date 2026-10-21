package com.shinhan.pg.payment.adapter.out.persistence;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlSourceFact;
import com.shinhan.pg.payment.application.port.out.AmlFactReadPort;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class AmlFactReadAdapter implements AmlFactReadPort {
    private final JdbcTemplate jdbc;

    public AmlFactReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AmlSourceFact read(UUID id) {
        var facts =
                jdbc.query(
                        "select o.*,p.actor_user_id,p.source_account_id,p.destination_account_id"
                                + " from payment_operations o join payments p on p.id=o.payment_id"
                                + " where o.id=?",
                        (r, n) ->
                                new AmlSourceFact(
                                        id,
                                        UUID.fromString(r.getString("id")),
                                        "PAYMENT_" + r.getString("kind"),
                                        UUID.fromString(r.getString("actor_user_id")),
                                        UUID.fromString(r.getString("source_account_id")),
                                        UUID.fromString(r.getString("destination_account_id")),
                                        r.getBigDecimal("amount"),
                                        r.getString("currency"),
                                        r.getTimestamp("created_at").toInstant(),
                                        "APPROVAL".equals(r.getString("kind"))
                                                ? null
                                                : UUID.fromString(r.getString("payment_id")),
                                        r.getString("status")),
                        id.toString());
        if (facts.isEmpty()) throw AmlProblem.missing();
        return facts.getFirst();
    }
}

package com.shinhan.pg.transfer.adapter.out.persistence;

import com.shinhan.pg.common.amlcontracts.AmlProblem;
import com.shinhan.pg.common.amlcontracts.AmlSourceFact;
import com.shinhan.pg.transfer.application.port.out.AmlFactReadPort;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class AmlFactReadAdapter implements AmlFactReadPort {
    private final JdbcTemplate jdbc;

    public AmlFactReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public AmlSourceFact read(UUID id) {
        var domestic =
                jdbc.query(
                        "select * from transfers where transaction_id=?",
                        (r, n) ->
                                new AmlSourceFact(
                                        id,
                                        UUID.fromString(r.getString("id")),
                                        "DOMESTIC_TRANSFER",
                                        UUID.fromString(r.getString("actor_user_id")),
                                        UUID.fromString(r.getString("from_account_id")),
                                        UUID.fromString(r.getString("to_account_id")),
                                        r.getBigDecimal("amount"),
                                        r.getString("currency"),
                                        r.getTimestamp("created_at").toInstant(),
                                        null,
                                        r.getString("status")),
                        id.toString());
        if (!domestic.isEmpty()) return domestic.getFirst();
        var overseas =
                jdbc.query(
                        "select * from overseas_transfers where id=? or final_transaction_id=?",
                        (r, n) ->
                                new AmlSourceFact(
                                        id,
                                        UUID.fromString(r.getString("id")),
                                        "OVERSEAS_TRANSFER",
                                        UUID.fromString(r.getString("actor_user_id")),
                                        UUID.fromString(r.getString("from_account_id")),
                                        null,
                                        r.getBigDecimal("amount"),
                                        r.getString("currency"),
                                        r.getTimestamp("created_at").toInstant(),
                                        null,
                                        r.getString("status")),
                        id.toString(),
                        id.toString());
        if (overseas.isEmpty()) throw AmlProblem.missing();
        return overseas.getFirst();
    }
}

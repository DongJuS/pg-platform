package com.shinhan.pg.aml.adapter.out.persistence;

import com.shinhan.pg.aml.application.port.out.AmlCaseRepository;
import com.shinhan.pg.aml.domain.model.AmlCase;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

public final class AmlCasePersistenceAdapter implements AmlCaseRepository {
    private final JdbcTemplate jdbc;

    public AmlCasePersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AmlCase> findById(UUID id) {
        return jdbc
                .query(
                        "select * from aml_cases where created_at>=now()-interval '48 hours' and"
                                + " id=?",
                        (r, n) ->
                                new AmlCase(
                                        UUID.fromString(r.getString("id")),
                                        r.getString("case_key"),
                                        UUID.fromString(r.getString("transaction_id")),
                                        r.getString("rule_version"),
                                        r.getInt("risk_score"),
                                        r.getString("status"),
                                        r.getTimestamp("created_at").toInstant()),
                        id.toString())
                .stream()
                .findFirst();
    }
}

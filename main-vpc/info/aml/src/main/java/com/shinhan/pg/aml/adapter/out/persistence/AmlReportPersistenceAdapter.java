package com.shinhan.pg.aml.adapter.out.persistence;

import com.shinhan.pg.aml.application.port.out.AmlReportRepository;
import com.shinhan.pg.aml.domain.model.AmlReport;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

public final class AmlReportPersistenceAdapter implements AmlReportRepository {
    private final JdbcTemplate jdbc;

    public AmlReportPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AmlReport> findById(UUID id) {
        return jdbc
                .query(
                        "select * from aml_reports where created_at>=now()-interval '48 hours' and"
                                + " id=?",
                        (r, n) ->
                                new AmlReport(
                                        UUID.fromString(r.getString("id")),
                                        UUID.fromString(r.getString("case_id")),
                                        r.getString("report_key"),
                                        "STR_SIMULATION",
                                        r.getString("status"),
                                        r.getString("receipt_reference")),
                        id.toString())
                .stream()
                .findFirst();
    }
}

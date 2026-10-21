package com.shinhan.pg.aml.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.aml.application.port.out.DetectionRuleRepository;
import com.shinhan.pg.aml.domain.model.DetectionRule;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

public final class DetectionRulePersistenceAdapter implements DetectionRuleRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public DetectionRulePersistenceAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<DetectionRule> findByCodeAndVersion(String code, String version) {
        return jdbc
                .query(
                        "select document from aml_rules where code=? and version=?",
                        (r, n) -> {
                            try {
                                return json.readValue(r.getString(1), DetectionRule.class);
                            } catch (Exception e) {
                                throw new IllegalStateException("AML storage unavailable");
                            }
                        },
                        code,
                        version)
                .stream()
                .findFirst();
    }
}

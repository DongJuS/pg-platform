package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.domain.model.DetectionRule;

import java.util.Optional;

public interface DetectionRuleRepository {
    Optional<DetectionRule> findByCodeAndVersion(String code, String version);
}

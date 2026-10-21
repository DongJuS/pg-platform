package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.domain.model.AmlReport;

import java.util.Optional;
import java.util.UUID;

public interface AmlReportRepository {
    Optional<AmlReport> findById(UUID id);
}

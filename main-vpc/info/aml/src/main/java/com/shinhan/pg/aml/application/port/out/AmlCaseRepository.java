package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.domain.model.AmlCase;

import java.util.Optional;
import java.util.UUID;

public interface AmlCaseRepository {
    Optional<AmlCase> findById(UUID id);
}

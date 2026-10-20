package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.domain.model.PgRequest;
import com.shinhan.pg.fepadapter.domain.model.PgResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PgProviderPort {
    PgResult submit(PgRequest request);

    Optional<PgResult> query(UUID id);

    List<PgResult> results(LocalDate date);
}

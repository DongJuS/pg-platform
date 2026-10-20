package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.domain.model.PgRequest;
import com.shinhan.pg.fepadapter.domain.model.PgResult;

import java.util.UUID;

public interface PgReceiptStore {
    UUID reserve(PgRequest request);

    boolean claim(UUID operationId);

    UUID receipt(UUID operationId);

    PgResult result(UUID operationId);

    void result(PgResult result);

    void callback(UUID eventId, byte[] raw, String signature);
}

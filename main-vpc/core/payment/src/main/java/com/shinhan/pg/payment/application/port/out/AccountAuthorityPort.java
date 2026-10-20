package com.shinhan.pg.payment.application.port.out;

import java.util.UUID;

/** Validate source ownership, both active accounts and exact currency through Account REST. */
public interface AccountAuthorityPort {
    void verify(UUID actor, UUID source, UUID destination, String currency);
}

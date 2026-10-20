package com.shinhan.pg.ledger.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** Internal transfer-owned request; actor is rechecked at the Account source before DB locking. */
public record ReserveOverseasCommand(
        UUID transferId,
        String uetr,
        UUID actorUserId,
        UUID fromAccountId,
        BigDecimal amount,
        String currency) {}

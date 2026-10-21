package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.common.amlcontracts.LedgerEvidence;

import java.util.UUID;

public interface AmlFactReadPort {
    LedgerEvidence read(UUID transactionId);
}

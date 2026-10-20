package com.shinhan.pg.ledger.application.port.out;

import com.shinhan.pg.ledger.application.readmodel.JournalAudit;

import java.util.UUID;

public interface JournalAuditPort {
    JournalAudit read(UUID transactionId);
}

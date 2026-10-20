package com.shinhan.pg.ledger.application.port.in;

import com.shinhan.pg.ledger.application.readmodel.JournalAudit;

import java.util.UUID;

public interface GetJournalAuditUseCase {
    JournalAudit get(UUID transactionId);
}

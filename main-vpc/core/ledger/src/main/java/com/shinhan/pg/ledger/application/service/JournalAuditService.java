package com.shinhan.pg.ledger.application.service;

import com.shinhan.pg.ledger.application.port.in.GetJournalAuditUseCase;
import com.shinhan.pg.ledger.application.port.out.JournalAuditPort;
import com.shinhan.pg.ledger.application.readmodel.JournalAudit;

import java.util.UUID;

public final class JournalAuditService implements GetJournalAuditUseCase {
    private final JournalAuditPort port;

    public JournalAuditService(JournalAuditPort port) {
        this.port = port;
    }

    public JournalAudit get(UUID id) {
        return port.read(id);
    }
}

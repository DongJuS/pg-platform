package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.ledger.application.port.in.GetJournalAuditUseCase;
import com.shinhan.pg.ledger.application.readmodel.JournalAudit;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("skeleton")
public final class JournalAuditController {
    private final GetJournalAuditUseCase useCase;

    public JournalAuditController(GetJournalAuditUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/internal/v1/ledger/transactions/{id}/postings")
    public JournalAudit get(@PathVariable UUID id) {
        return useCase.get(id);
    }
}

package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.common.amlcontracts.LedgerEvidence;
import com.shinhan.pg.ledger.application.service.AmlFactReadService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * MVP6: GET source-owned facts, dedicated AML credential, IDs/financial evidence only. 404 stays
 * unresolved at AML.
 */
@RestController
@Profile("aml-test")
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public final class AmlFactController {
    private final AmlFactReadService service;

    public AmlFactController(AmlFactReadService service) {
        this.service = service;
    }

    @GetMapping("/internal/v1/ledger/aml-facts/{transactionId}")
    public LedgerEvidence get(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID transactionId) {
        return service.read(caller, transactionId);
    }
}

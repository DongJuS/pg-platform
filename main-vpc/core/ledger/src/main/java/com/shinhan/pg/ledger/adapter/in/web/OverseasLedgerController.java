package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.ledger.application.command.ReserveOverseasCommand;
import com.shinhan.pg.ledger.application.result.PostJournalResult;
import com.shinhan.pg.ledger.application.service.ReserveOverseasService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST internal reservation → Account ownership → atomic hold posting + row + ledger.posted. 400
 * input, 403 caller/owner, 409 changed identity, 422 insufficient funds. Receipt is not final
 * settlement.
 */
@RestController
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class OverseasLedgerController {
    private final ReserveOverseasService service;

    public OverseasLedgerController(ReserveOverseasService service) {
        this.service = service;
    }

    @PostMapping("/internal/v1/ledger/overseas-reservations")
    public PostJournalResult reserve(
            @RequestAttribute("callerServiceId") String caller,
            @RequestBody ReserveOverseasCommand request) {
        if (!"transfer".equals(caller)) throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        return service.reserve(request);
    }
}

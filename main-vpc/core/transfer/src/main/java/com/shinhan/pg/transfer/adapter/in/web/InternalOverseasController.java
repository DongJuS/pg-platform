package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.Remittance;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.transfer.application.port.out.OverseasTransferStore;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class InternalOverseasController {
    private final OverseasTransferStore store;

    public InternalOverseasController(OverseasTransferStore store) {
        this.store = store;
    }

    @GetMapping("/internal/v1/transfers/overseas/{id}/instruction")
    public Remittance instruction(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID id) {
        if (!"swift".equals(caller)) throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
        var row =
                store.find(id)
                        .orElseThrow(() -> new SwiftProblem("NOT_FOUND", ErrorType.NOT_FOUND));
        if (!java.util.Set.of("PROCESSING", "REVIEW", "COMPLETED", "REJECTED")
                .contains(row.status())) throw SwiftProblem.conflict();
        if (row.remittance() == null)
            throw new SwiftProblem("PRIVATE_DATA_EXPIRED", ErrorType.NOT_FOUND);
        return row.remittance();
    }
}

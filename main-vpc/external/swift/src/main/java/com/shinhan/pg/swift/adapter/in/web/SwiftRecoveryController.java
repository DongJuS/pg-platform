package com.shinhan.pg.swift.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.swift.application.command.RetrySwiftCommand;
import com.shinhan.pg.swift.application.port.out.SwiftSubmissionStore;
import com.shinhan.pg.swift.application.service.SwiftApplicationService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manual query and real retransmission are separate actions. Requires a dedicated operator caller
 * credential.
 */
@RestController
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class SwiftRecoveryController {
    private final SwiftApplicationService service;
    private final SwiftSubmissionStore store;

    public SwiftRecoveryController(SwiftApplicationService service, SwiftSubmissionStore store) {
        this.service = service;
        this.store = store;
    }

    @PostMapping("/internal/v1/swift/transfers/{uetr}/result-queries")
    public Map<String, String> query(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String uetr) {
        authorize(caller);
        service.query(uetr);
        return Map.of("status", store.find(uetr).orElseThrow(SwiftProblem::invalid).status());
    }

    @PostMapping("/internal/v1/swift/transfers/{uetr}/retransmissions")
    public Map<String, String> resend(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String uetr) {
        authorize(caller);
        var r = store.find(uetr).orElseThrow(SwiftProblem::invalid);
        service.retrySwift(new RetrySwiftCommand(UUID.randomUUID(), r.transferId(), uetr));
        return Map.of("status", store.find(uetr).orElseThrow().status());
    }

    @GetMapping("/internal/v1/swift/transfers/{uetr}/messages")
    public List<String> history(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String uetr) {
        authorize(caller);
        return service.history(uetr, true);
    }

    private static void authorize(String caller) {
        if (!"swift-operations".equals(caller))
            throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
    }
}

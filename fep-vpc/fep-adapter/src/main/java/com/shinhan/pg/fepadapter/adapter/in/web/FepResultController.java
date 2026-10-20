package com.shinhan.pg.fepadapter.adapter.in.web;

import com.shinhan.pg.common.commonerror.ErrorType;
import com.shinhan.pg.common.swiftmessages.SwiftProblem;
import com.shinhan.pg.fepadapter.application.result.SendSwiftMessageResult;
import com.shinhan.pg.fepadapter.application.service.SwiftFepService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal SWIFT result polling, explicit retransmission and authenticated Mock callbacks. Query
 * does not transmit a payment. Reports are schema validated and correlated before being stored.
 */
@RestController
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class FepResultController {
    private final SwiftFepService service;

    public FepResultController(SwiftFepService service) {
        this.service = service;
    }

    @GetMapping("/internal/v1/fep/swift/messages/{uetr}/result")
    public Reports result(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String uetr) {
        authorize(caller, "swift");
        var reports = service.query(uetr);
        return new Reports(
                reports.isEmpty() ? null : reports.get(reports.size() - 1),
                reports,
                service.transmissionStatus(uetr));
    }

    @PostMapping("/internal/v1/fep/swift/messages/{uetr}/retransmissions")
    public SendSwiftMessageResult resend(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String uetr) {
        authorize(caller, "swift");
        return service.retry(uetr);
    }

    @PostMapping("/internal/v1/fep/swift/messages/{uetr}/results")
    public void callback(
            @RequestAttribute("callerServiceId") String caller,
            @PathVariable String uetr,
            @RequestBody Report request) {
        authorize(caller, "mock-swift");
        service.callback(uetr, request.pacs002());
    }

    private static void authorize(String actual, String expected) {
        if (!expected.equals(actual)) throw new SwiftProblem("FORBIDDEN", ErrorType.FORBIDDEN);
    }

    public record Reports(String pacs002, List<String> reports, String transmissionStatus) {
        @Override
        public String toString() {
            return "SwiftReports[REDACTED]";
        }
    }

    public record Report(String pacs002) {
        @Override
        public String toString() {
            return "SwiftReport[REDACTED]";
        }
    }
}

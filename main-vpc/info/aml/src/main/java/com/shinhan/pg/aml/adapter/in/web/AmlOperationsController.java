package com.shinhan.pg.aml.adapter.in.web;

import com.shinhan.pg.aml.application.service.AmlApplicationService;
import com.shinhan.pg.common.amlcontracts.KofiuResult;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Development operations identity only. Read APIs never mutate/report/approve a case. */
@RestController
@Profile("aml-test")
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public final class AmlOperationsController {
    private final AmlApplicationService service;

    public AmlOperationsController(AmlApplicationService service) {
        this.service = service;
    }

    @GetMapping("/internal/v1/aml/cases/{id}")
    public com.shinhan.pg.aml.application.readmodel.AmlCaseDetails get(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID id) {
        return service.details(caller, id);
    }

    @GetMapping("/internal/v1/aml/cases/{id}/report")
    public KofiuResult report(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID id) {
        return service.report(caller, id);
    }

    @PostMapping("/internal/v1/aml/cases/{id}/report-queries")
    public KofiuResult query(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID id) {
        return service.query(caller, id);
    }

    @PostMapping("/internal/v1/aml/cases/{id}/report-retransmissions")
    public KofiuResult retry(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID id) {
        return service.retransmit(caller, id);
    }

    @PostMapping("/internal/v1/aml/reevaluations")
    public void reevaluate(@RequestAttribute("callerServiceId") String caller) {
        service.reevaluate(caller);
    }

    @PostMapping("/internal/v1/aml/cases/{id}/report-amendments")
    public KofiuResult amend(
            @RequestAttribute("callerServiceId") String caller,
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestBody Amendment request) {
        return service.amend(caller, id, request.previousReportId());
    }

    public record Amendment(UUID previousReportId) {}

    @GetMapping("/internal/v1/aml/cases/{id}/report-document")
    public com.shinhan.pg.common.amlcontracts.AmlReportDocument document(
            @RequestAttribute("callerServiceId") String caller, @PathVariable UUID id) {
        return service.document(caller, id);
    }
}

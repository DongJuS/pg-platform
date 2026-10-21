package com.shinhan.pg.fepadapter.adapter.in.web;

import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.fepadapter.application.service.KofiuFepService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** MVP6-F03/F05: dedicated authenticated AML caller; 202 means durable FEP receipt only. */
@RestController
@Profile("aml-test")
@ConditionalOnProperty(name = "pg.aml.enabled", havingValue = "true")
public final class FepKofiuController {
    private final KofiuFepService service;

    public FepKofiuController(KofiuFepService service) {
        this.service = service;
    }

    @PostMapping("/internal/v1/fep/kofiu/reports")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public KofiuResult submit(
            @RequestAttribute("callerServiceId") String caller, @RequestBody AmlReportDocument d) {
        return service.submit(caller, d);
    }

    @GetMapping("/internal/v1/fep/kofiu/reports/{key}")
    public KofiuResult query(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String key) {
        return service.query(caller, key);
    }

    @PostMapping("/internal/v1/fep/kofiu/reports/{key}/retransmissions")
    public KofiuResult retry(
            @RequestAttribute("callerServiceId") String caller, @PathVariable String key) {
        return service.retry(caller, key);
    }
}

package com.shinhan.pg.fepadapter.adapter.in.web;

import com.shinhan.pg.fepadapter.application.service.PgGatewayApplicationService;
import com.shinhan.pg.fepadapter.domain.model.PgRequest;
import com.shinhan.pg.fepadapter.domain.model.PgResult;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("skeleton")
public final class FepPgController {
    private final PgGatewayApplicationService service;

    public FepPgController(PgGatewayApplicationService service) {
        this.service = service;
    }

    @PostMapping("/internal/v1/fep/pg/operations")
    public PgGatewayApplicationService.Receipt submit(@RequestBody PgRequest request) {
        return service.submit(request);
    }

    @GetMapping("/internal/v1/fep/pg/operations/{id}")
    public PgGatewayApplicationService.Receipt query(@PathVariable UUID id) {
        return service.query(id);
    }

    @GetMapping("/internal/v1/fep/pg/results")
    public List<PgResult> results(@RequestParam LocalDate businessDate) {
        return service.results(businessDate);
    }

    @PostMapping("/callbacks/v1/pg")
    public void callback(
            @RequestBody byte[] raw, @RequestHeader("X-PG-Signature") String signature) {
        service.receive(raw, signature);
    }
}

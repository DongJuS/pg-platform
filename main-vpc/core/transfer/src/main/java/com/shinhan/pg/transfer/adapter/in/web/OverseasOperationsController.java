package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.transfer.application.service.OverseasOperationsService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Latest user instruction activates the former F07 hold for administrators; ordinary owners receive
 * masked history.
 */
@RestController
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class OverseasOperationsController {
    private final OverseasOperationsService service;

    public OverseasOperationsController(OverseasOperationsService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/transfers/overseas/{id}/messages")
    public ResponseEntity<List<String>> history(
            @RequestHeader(name = "X-User-Id", required = false) UUID actor,
            @PathVariable UUID id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.history(actor, id));
    }

    @PostMapping("/api/v1/transfers/overseas/{id}/result-queries")
    public Map<String, String> query(
            @RequestHeader(name = "X-User-Id", required = false) UUID actor,
            @PathVariable UUID id) {
        return Map.of("externalStatus", service.action(actor, id, false));
    }

    @PostMapping("/api/v1/transfers/overseas/{id}/retransmissions")
    public Map<String, String> resend(
            @RequestHeader(name = "X-User-Id", required = false) UUID actor,
            @PathVariable UUID id) {
        return Map.of("externalStatus", service.action(actor, id, true));
    }
}

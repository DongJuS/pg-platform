package com.shinhan.pg.transfer.adapter.in.batch;

import com.shinhan.pg.transfer.application.service.OverseasTransferService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("skeleton")
@ConditionalOnProperty(name = "pg.swift.enabled", havingValue = "true")
public final class OverseasRecoveryBatch {
    private final OverseasTransferService service;

    public OverseasRecoveryBatch(OverseasTransferService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${pg.swift.recovery-interval-ms:10000}")
    public void recover() {
        service.recoverPending();
    }
}

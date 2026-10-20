package com.shinhan.pg.transfer.application.readmodel;

import com.shinhan.pg.common.swiftmessages.Remittance;

import java.util.UUID;

public record OverseasSnapshot(
        UUID transferId,
        UUID actorUserId,
        UUID fromAccountId,
        String uetr,
        String requestHash,
        String status,
        Remittance remittance) {
    @Override
    public String toString() {
        return "OverseasSnapshot[REDACTED]";
    }
}

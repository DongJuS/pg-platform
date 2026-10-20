package com.shinhan.pg.fepadapter.application.readmodel;

import java.util.UUID;

public record SwiftReceipt(
        UUID transferId,
        UUID messageId,
        String uetr,
        String reference,
        String xml,
        String outcome,
        int attempts) {
    @Override
    public String toString() {
        return "SwiftReceipt[REDACTED]";
    }
}

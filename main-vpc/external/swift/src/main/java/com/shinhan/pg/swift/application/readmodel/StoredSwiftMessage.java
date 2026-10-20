package com.shinhan.pg.swift.application.readmodel;

import java.util.UUID;

public record StoredSwiftMessage(
        UUID transferId, String uetr, String reference, String xml, String status, int attempts) {
    @Override
    public String toString() {
        return "StoredSwiftMessage[REDACTED]";
    }
}

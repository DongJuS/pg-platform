package com.shinhan.pg.ledger.application.readmodel;
import java.util.UUID;
/** Account service owns identity, ownership, currency and status. No cross-database reads. */
public record AccountAccessSnapshot(UUID accountId, UUID ownerId, String currency, String status) {}

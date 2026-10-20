package com.shinhan.pg.aigateway.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

/** Physical PostgreSQL row, intentionally incapable of holding prompts or restoration values. */
public record AiRequestAuditEntity(UUID requestId, String callerServiceId, String decision,
        String reason, int nameCount, int rrnCount, int accountCount, int phoneCount, int emailCount, int cardCount,
        String policyVersion, Instant createdAt) {}

package com.shinhan.pg.aigateway.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Minimal preparation evidence. No prompt/hash, credentials, restoration mapping or model usage. */
public record AiRequestAudit(UUID requestId, String callerServiceId,
        PreparationDecision decision, PreparationReason reason, Map<PiiType, Integer> maskedCounts,
        String policyVersion, Instant createdAt) {
    public AiRequestAudit { maskedCounts = Map.copyOf(maskedCounts); }
    @Override public String toString() { return "AiRequestAudit[" + requestId + "," + decision + "]"; }
}

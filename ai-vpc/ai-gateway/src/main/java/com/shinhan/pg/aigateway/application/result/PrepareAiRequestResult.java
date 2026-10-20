package com.shinhan.pg.aigateway.application.result;

import com.shinhan.pg.aigateway.domain.model.PiiType;
import com.shinhan.pg.aigateway.domain.model.PreparationDecision;
import com.shinhan.pg.aigateway.domain.model.PreparationReason;
import java.util.Map;
import java.util.UUID;

/** Readiness means pre-transfer checks only. It is not an LLM response or a transfer permit. */
public record PrepareAiRequestResult(UUID requestId, PreparationDecision status,
        PreparationReason reason, String maskedPrompt, Map<PiiType, Integer> maskedCounts,
        String policyVersion) {
    public PrepareAiRequestResult { maskedCounts = Map.copyOf(maskedCounts); }
    @Override public String toString() { return "PrepareAiRequestResult[" + requestId + "," + status + "]"; }
}

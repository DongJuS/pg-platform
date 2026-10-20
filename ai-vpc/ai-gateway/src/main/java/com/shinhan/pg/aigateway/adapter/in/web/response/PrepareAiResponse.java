package com.shinhan.pg.aigateway.adapter.in.web.response;

import com.shinhan.pg.aigateway.application.result.PrepareAiRequestResult;
import com.shinhan.pg.aigateway.domain.model.PiiType;
import com.shinhan.pg.aigateway.domain.model.PreparationDecision;
import com.shinhan.pg.aigateway.domain.model.PreparationReason;
import java.util.Map;
import java.util.UUID;

public record PrepareAiResponse(UUID requestId, PreparationDecision status, PreparationReason reason,
        String maskedPrompt, Map<PiiType, Integer> maskedCounts, String policyVersion, boolean externalTransferPerformed) {
    public static PrepareAiResponse from(PrepareAiRequestResult r) {
        return new PrepareAiResponse(r.requestId(), r.status(), r.reason(), r.maskedPrompt(),
                r.maskedCounts(), r.policyVersion(), false);
    }
    @Override public String toString() { return "PrepareAiResponse[" + requestId + "," + status + "]"; }
}

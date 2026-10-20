package com.shinhan.pg.aigateway.adapter.out.inspection;

import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;
import com.shinhan.pg.aigateway.domain.model.SensitiveValue;
import com.shinhan.pg.aigateway.domain.policy.PiiMaskingPolicy;
import java.util.List;

/** No remote inspection service or model is contacted. */
public final class RuleBasedPiiInspectionAdapter implements PiiInspectionPort {
    private final PiiMaskingPolicy policy = new PiiMaskingPolicy();
    @Override public boolean isClear(String maskedPrompt, List<SensitiveValue> sensitiveValues) {
        return policy.residualFree(maskedPrompt, sensitiveValues);
    }
}

package com.shinhan.pg.aigateway.adapter.out.inspection;

import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;
import com.shinhan.pg.aigateway.domain.model.SensitiveValue;
import java.util.List;

/** Historical inference placeholder; never registered by preparation configuration. */
public final class PiiInspectionSkeleton implements PiiInspectionPort {
    @Override public boolean isClear(String maskedPrompt, List<SensitiveValue> sensitiveValues) {
        throw new UnsupportedOperationException("Deferred inference inspection");
    }
}

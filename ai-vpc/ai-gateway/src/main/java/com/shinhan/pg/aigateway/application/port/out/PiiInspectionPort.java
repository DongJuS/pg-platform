package com.shinhan.pg.aigateway.application.port.out;

import com.shinhan.pg.aigateway.domain.model.SensitiveValue;
import java.util.List;

/** True only if supported PII patterns/hints are absent. Errors must fail closed. */
public interface PiiInspectionPort {
    boolean isClear(String maskedPrompt, List<SensitiveValue> sensitiveValues);
}

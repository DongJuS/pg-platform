package com.shinhan.pg.aigateway.application.command;

import com.shinhan.pg.aigateway.domain.model.SensitiveValue;
import java.util.List;

/** callerServiceId comes exclusively from the authenticated request context. */
public record PrepareAiRequestCommand(String callerServiceId, String inputFormat, String prompt,
        List<SensitiveValue> sensitiveValues) {
    @Override public String toString() { return "PrepareAiRequestCommand[REDACTED]"; }
}

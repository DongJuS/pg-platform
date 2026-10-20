package com.shinhan.pg.aigateway.adapter.in.web.request;

import com.shinhan.pg.aigateway.domain.model.SensitiveValue;
import java.util.List;

/** Raw TEXT only; names and ambiguous account identifiers require an explicit caller inventory. */
public record PrepareAiRequest(String inputFormat, String prompt, List<SensitiveValue> sensitiveValues) {
    @Override public String toString() { return "PrepareAiRequest[REDACTED]"; }
}

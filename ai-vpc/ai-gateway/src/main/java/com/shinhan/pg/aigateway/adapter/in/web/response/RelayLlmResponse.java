package com.shinhan.pg.aigateway.adapter.in.web.response;

import java.util.UUID;

/** [MVP3-F01] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RelayLlmResponse(
        UUID requestId, String output, Integer inputTokens, Integer outputTokens) {
    @Override
    public String toString() {
        return "RelayLlmResponse[REDACTED]";
    }
}

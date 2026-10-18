package com.shinhan.pg.aigateway.adapter.in.web.response;

import java.time.Instant;
import java.util.UUID;

/** [MVP3-F02] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetAiRequestResponse(
        UUID requestId, String decision, String reasonCode, Instant createdAt) {}

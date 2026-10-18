package com.shinhan.pg.aigateway.adapter.in.web.response;

/** [MVP3-F04] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetAiSecurityStatisticsResponse(Long requestCount, Long blockedCount) {}

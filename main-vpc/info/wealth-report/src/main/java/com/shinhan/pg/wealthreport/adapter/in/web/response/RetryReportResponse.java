package com.shinhan.pg.wealthreport.adapter.in.web.response;

import java.util.UUID;

/** [MVP4-F04] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RetryReportResponse(UUID reportId, String status) {}

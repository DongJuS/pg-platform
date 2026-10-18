package com.shinhan.pg.aml.adapter.in.web.response;

import java.util.UUID;

/** [MVP6-F03] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record SubmitAmlReportResponse(UUID reportId, String status, String receiptReference) {}

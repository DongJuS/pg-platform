package com.shinhan.pg.wealthreport.adapter.in.web.response;

import com.shinhan.pg.wealthreport.application.readmodel.ReportSummary;

import java.util.List;

/** [MVP4-F03] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ListReportsResponse(List<ReportSummary> reports, Integer nextPage) {}

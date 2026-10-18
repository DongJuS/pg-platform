package com.shinhan.pg.wealthreport.application.result;

import com.shinhan.pg.wealthreport.application.readmodel.ReportSummary;

import java.util.List;

/** [MVP4-F03] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record ListReportsResult(List<ReportSummary> reports, Integer nextPage) {}

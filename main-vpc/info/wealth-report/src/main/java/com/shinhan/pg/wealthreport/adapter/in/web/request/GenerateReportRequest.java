package com.shinhan.pg.wealthreport.adapter.in.web.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** [MVP4-F01] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record GenerateReportRequest(
        Instant from, Instant to, List<UUID> accountIds, Instant asOf) {}

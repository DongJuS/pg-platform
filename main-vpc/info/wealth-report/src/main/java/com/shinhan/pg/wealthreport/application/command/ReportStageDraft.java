package com.shinhan.pg.wealthreport.application.command;

import java.time.Instant;
import java.util.UUID;

/** 내부 진행 기록 초안. stageCode·DB 스키마·재개 정책은 후속 구현이다. */
public record ReportStageDraft(UUID reportId, Instant asOf, String stageCode) {}

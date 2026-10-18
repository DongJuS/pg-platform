package com.shinhan.pg.wealthreport.application.command;

import java.util.UUID;

/** 중단 작업 재조회·재개 입력. 실제 범위·기간/스케줄은 후속 계약이다. */
public record RecoverReportGenerationCommand(UUID requestId, UUID resourceId) {}

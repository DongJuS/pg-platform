package com.shinhan.pg.aml.application.command;

import java.util.UUID;

/** 자동 보고 전송의 안정적 업무 식별자 초안. KoFIU wire payload는 후속 구현이다. */
public record AmlReportDraft(UUID reportId, String reportKey) {}

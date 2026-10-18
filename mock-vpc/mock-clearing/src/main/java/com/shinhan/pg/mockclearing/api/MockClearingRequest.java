package com.shinhan.pg.mockclearing.api;

import com.shinhan.pg.mockclearing.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 시나리오 선택 초안. 금융 전문/HTTP 요청 계약이 아니다. */
public record MockClearingRequest(UUID requestId, ScenarioMode mode) {}

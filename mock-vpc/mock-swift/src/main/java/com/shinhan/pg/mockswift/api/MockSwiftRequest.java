package com.shinhan.pg.mockswift.api;

import com.shinhan.pg.mockswift.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 시나리오 선택 초안. 금융 전문/HTTP 요청 계약이 아니다. */
public record MockSwiftRequest(UUID requestId, ScenarioMode mode) {}

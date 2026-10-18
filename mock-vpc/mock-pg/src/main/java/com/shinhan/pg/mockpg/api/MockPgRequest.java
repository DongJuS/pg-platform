package com.shinhan.pg.mockpg.api;

import com.shinhan.pg.mockpg.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 시나리오 선택 초안. 금융 전문/HTTP 요청 계약이 아니다. */
public record MockPgRequest(UUID requestId, ScenarioMode mode) {}

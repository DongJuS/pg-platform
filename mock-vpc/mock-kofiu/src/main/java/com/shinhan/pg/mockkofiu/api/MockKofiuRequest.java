package com.shinhan.pg.mockkofiu.api;

import com.shinhan.pg.mockkofiu.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 시나리오 선택 초안. 금융 전문/HTTP 요청 계약이 아니다. */
public record MockKofiuRequest(UUID requestId, ScenarioMode mode) {}

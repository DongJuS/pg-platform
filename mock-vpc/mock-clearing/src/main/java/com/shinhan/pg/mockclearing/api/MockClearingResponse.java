package com.shinhan.pg.mockclearing.api;

import com.shinhan.pg.mockclearing.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 Mock 결과 초안. 금융 승인·송금 성공을 의미하지 않는다. */
public record MockClearingResponse(UUID requestId, ScenarioMode mode) {}

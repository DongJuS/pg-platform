package com.shinhan.pg.mockswift.api;

import com.shinhan.pg.mockswift.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 Mock 결과 초안. 금융 승인·송금 성공을 의미하지 않는다. */
public record MockSwiftResponse(UUID requestId, ScenarioMode mode) {}

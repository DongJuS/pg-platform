package com.shinhan.pg.mockkofiu.api;

import com.shinhan.pg.mockkofiu.scenario.ScenarioMode;

import java.util.UUID;

/** 내부 Mock 결과 초안. 금융 승인·송금 성공을 의미하지 않는다. */
public record MockKofiuResponse(UUID requestId, ScenarioMode mode) {}

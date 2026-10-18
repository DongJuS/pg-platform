package com.shinhan.pg.mockpg.scenario;

import java.util.UUID;

/** Mock 내부 초안. 외부 금융기관 응답 형식은 아직 정하지 않았다. */
public record MockPgScenarioResult(UUID requestId, ScenarioMode mode) {}

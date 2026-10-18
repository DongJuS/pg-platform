package com.shinhan.pg.mockpg.api;

import com.shinhan.pg.mockpg.scenario.MockPgScenarioSkeleton;

import java.util.Objects;

/** Controller/경로를 등록하지 않는 Java skeleton. */
public final class MockPgApiSkeleton {
    private final MockPgScenarioSkeleton scenario;

    public MockPgApiSkeleton(MockPgScenarioSkeleton scenario) {
        this.scenario = Objects.requireNonNull(scenario);
    }

    public MockPgResponse handle(MockPgRequest request) {
        var result = scenario.run(request.requestId(), request.mode());
        return new MockPgResponse(result.requestId(), result.mode());
    }
}

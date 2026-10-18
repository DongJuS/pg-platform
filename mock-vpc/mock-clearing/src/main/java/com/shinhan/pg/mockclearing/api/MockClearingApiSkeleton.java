package com.shinhan.pg.mockclearing.api;

import com.shinhan.pg.mockclearing.scenario.MockClearingScenarioSkeleton;

import java.util.Objects;

/** Controller/경로를 등록하지 않는 Java skeleton. */
public final class MockClearingApiSkeleton {
    private final MockClearingScenarioSkeleton scenario;

    public MockClearingApiSkeleton(MockClearingScenarioSkeleton scenario) {
        this.scenario = Objects.requireNonNull(scenario);
    }

    public MockClearingResponse handle(MockClearingRequest request) {
        var result = scenario.run(request.requestId(), request.mode());
        return new MockClearingResponse(result.requestId(), result.mode());
    }
}

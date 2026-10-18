package com.shinhan.pg.mockswift.api;

import com.shinhan.pg.mockswift.scenario.MockSwiftScenarioSkeleton;

import java.util.Objects;

/** Controller/경로를 등록하지 않는 Java skeleton. */
public final class MockSwiftApiSkeleton {
    private final MockSwiftScenarioSkeleton scenario;

    public MockSwiftApiSkeleton(MockSwiftScenarioSkeleton scenario) {
        this.scenario = Objects.requireNonNull(scenario);
    }

    public MockSwiftResponse handle(MockSwiftRequest request) {
        var result = scenario.run(request.requestId(), request.mode());
        return new MockSwiftResponse(result.requestId(), result.mode());
    }
}

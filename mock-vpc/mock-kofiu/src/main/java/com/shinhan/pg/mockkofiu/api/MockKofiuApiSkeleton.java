package com.shinhan.pg.mockkofiu.api;

import com.shinhan.pg.mockkofiu.scenario.MockKofiuScenarioSkeleton;

import java.util.Objects;

/** Controller/경로를 등록하지 않는 Java skeleton. */
public final class MockKofiuApiSkeleton {
    private final MockKofiuScenarioSkeleton scenario;

    public MockKofiuApiSkeleton(MockKofiuScenarioSkeleton scenario) {
        this.scenario = Objects.requireNonNull(scenario);
    }

    public MockKofiuResponse handle(MockKofiuRequest request) {
        var result = scenario.run(request.requestId(), request.mode());
        return new MockKofiuResponse(result.requestId(), result.mode());
    }
}

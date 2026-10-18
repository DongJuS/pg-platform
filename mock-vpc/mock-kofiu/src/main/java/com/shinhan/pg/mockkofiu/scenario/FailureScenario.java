package com.shinhan.pg.mockkofiu.scenario;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP6-F03,MVP6-F05] FailureScenario 구현 자리.
 * TODO: 안정적 업무 ID에 대해 재현 가능한 결과·지연/실패를 생성한다. 현재 결과·외부 송신·sleep은 수행하지 않는다.
 * </pre>
 */
public final class FailureScenario {
    public void execute(String syntheticPayload) {
        throw new FeatureNotImplementedException("MOCK-KOFIU-SCENARIO");
    }
}

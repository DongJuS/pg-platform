package com.shinhan.pg.mockclearing.scenario;

/** 요청받은 시나리오 종류만 나타낸다. 승인/거절 등 외부 금융 상태가 아니다. */
public enum ScenarioMode {
    SUCCESS,
    DELAY,
    FAILURE
}

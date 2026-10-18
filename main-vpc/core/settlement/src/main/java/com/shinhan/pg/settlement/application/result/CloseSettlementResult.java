package com.shinhan.pg.settlement.application.result;

import java.util.UUID;

/** 내부 결과 초안. 실제 구현이 성공한 경우에만 반환하며 현재 스켈레톤은 결과를 만들지 않는다. */
public record CloseSettlementResult(UUID resourceId) {}

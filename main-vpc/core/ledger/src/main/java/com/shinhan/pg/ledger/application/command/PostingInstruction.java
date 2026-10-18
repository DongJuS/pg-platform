package com.shinhan.pg.ledger.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/** 분개 입력 초안. 부호·통화별 합계 0·잔액 조건을 원장 정책에서 검증한다. JSON wire 타입의 최종 계약은 미정. */
public record PostingInstruction(UUID accountId, BigDecimal signedAmount, String currency) {}

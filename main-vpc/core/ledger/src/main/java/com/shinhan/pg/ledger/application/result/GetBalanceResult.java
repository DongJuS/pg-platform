package com.shinhan.pg.ledger.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** [MVP1-F11] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record GetBalanceResult(UUID accountId, BigDecimal balance, String currency, Instant asOf) {}

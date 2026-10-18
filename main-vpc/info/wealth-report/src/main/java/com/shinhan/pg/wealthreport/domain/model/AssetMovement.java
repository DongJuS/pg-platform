package com.shinhan.pg.wealthreport.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

import java.time.Instant;
import java.util.UUID;

/** 계산/변환 값객체 초안. 금융 산식과 전이 동작은 후속 구현한다. */
public record AssetMovement(UUID transactionId, Money signedAmount, Instant occurredAt) {}

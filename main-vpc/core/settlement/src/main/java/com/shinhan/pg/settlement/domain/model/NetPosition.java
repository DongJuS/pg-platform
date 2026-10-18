package com.shinhan.pg.settlement.domain.model;

import com.shinhan.pg.common.commonmoney.Money;

/** 계산/변환 값객체 초안. 금융 산식과 전이 동작은 후속 구현한다. */
public record NetPosition(String institutionCode, Money amount) {}

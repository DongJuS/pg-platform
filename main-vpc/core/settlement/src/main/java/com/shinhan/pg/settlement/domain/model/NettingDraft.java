package com.shinhan.pg.settlement.domain.model;

import java.util.List;

/** 계산/변환 값객체 초안. 금융 산식과 전이 동작은 후속 구현한다. */
public record NettingDraft(List<NetPosition> positions, String calculationVersion) {}

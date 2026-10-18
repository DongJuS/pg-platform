package com.shinhan.pg.aml.domain.model;

/** [MVP6-F01,MVP6-F04] 순수 업무 모델 초안. 규칙·임계값·집계 창은 미정. 실제 CTR 규제 준수나 실제 현금 거래로 표방하지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record DetectionRule(String code, String version, String description, Integer score) {}

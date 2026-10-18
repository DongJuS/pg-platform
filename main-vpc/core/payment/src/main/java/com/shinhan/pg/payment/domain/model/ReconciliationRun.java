package com.shinhan.pg.payment.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** [MVP2-F08,MVP2-F09] 순수 업무 모델 초안. PG·내부 결제·원장 결과의 비교 근거를 남긴다. 자동 금액 보정·운영 종결 이력은 보류다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record ReconciliationRun(
        UUID id, LocalDate businessDate, String status, Instant startedAt, Instant completedAt) {}

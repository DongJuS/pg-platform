package com.shinhan.pg.settlement.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** [MVP7-F01,MVP7-F04,MVP7-F07,MVP7-F08] 순수 업무 모델 초안. 업무일 외에 회차·버전·고정 입력을 보존한다. 재실행이 입력 집합을 조용히 바꾸지 않게 한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record SettlementCycle(
        UUID id,
        LocalDate businessDate,
        Integer cycleNumber,
        Integer revision,
        Instant cutoffAt,
        String inputVersion,
        String status) {}

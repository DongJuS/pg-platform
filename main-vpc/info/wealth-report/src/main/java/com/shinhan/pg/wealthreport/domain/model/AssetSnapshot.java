package com.shinhan.pg.wealthreport.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** [MVP4-F05,MVP4-F09] 순수 업무 모델 초안. 지연·중복 거래를 처리할 자료 기준을 고정한다. 실시간 잔액을 섞지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record AssetSnapshot(
        UUID id, UUID userId, Instant asOf, String sourceVersion, List<UUID> accountIds) {}

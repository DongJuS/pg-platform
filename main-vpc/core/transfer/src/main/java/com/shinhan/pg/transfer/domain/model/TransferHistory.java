package com.shinhan.pg.transfer.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F16] 순수 업무 모델 초안. 전이와 사유·시점을 보존한다. 최종 상태의 역전·중복 완료를 막을 위치. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record TransferHistory(
        UUID id,
        UUID transferId,
        String previousStatus,
        String nextStatus,
        String reasonCode,
        Instant occurredAt) {}

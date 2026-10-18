package com.shinhan.pg.fepadapter.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP5-F04,MVP7-F03] 순수 업무 모델 초안. 응답 유실 후 같은 업무 식별자로 재전송·결과 확인. UETR 전체를 모든 메시지의 전역 unique로 쓰지 않는다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record FepTransmissionAttempt(
        UUID id,
        UUID messageId,
        Integer attemptNumber,
        String status,
        Instant requestedAt,
        Instant respondedAt) {}

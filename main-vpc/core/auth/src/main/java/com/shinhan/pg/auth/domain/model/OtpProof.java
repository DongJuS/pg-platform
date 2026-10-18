package com.shinhan.pg.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/** [MVP1-F06] 순수 업무 모델 초안. Mock OTP의 유효시간·일회 소모·실패 잠금 정책은 상세 계약 후 구현한다. 생성/전이 불변식은 후속 구현이며 ORM Entity가 아니다. */
public record OtpProof(
        UUID id,
        UUID userId,
        UUID transferDraftId,
        String proofHash,
        Instant expiresAt,
        Instant consumedAt) {}

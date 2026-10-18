package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.auth.domain.model.OtpProof;
import com.shinhan.pg.common.commonerror.ApplicationException;

import java.time.Instant;
import java.util.UUID;

/**
 * [MVP1-F06] Mock OTP proof의 일회 소모·만료·실패 잠금 규칙. 실제 OTP 공급자를 호출하지 않는다.
 * 잠금 한도 초안은 5회이며 계약 확정 후 조정한다.
 */
public final class OtpVerificationPolicy {
    private final int maxFailures;

    public OtpVerificationPolicy() {
        this(5);
    }

    public OtpVerificationPolicy(int maxFailures) {
        if (maxFailures < 1) {
            throw new IllegalArgumentException("maxFailures must be positive");
        }
        this.maxFailures = maxFailures;
    }

    public int maxFailures() {
        return maxFailures;
    }

    /** 초안 생성 전에 호출: 이 초안이 잠겨 있는지 확인한다. 잠겼으면 예외. */
    public void ensureNotLocked(UUID transferDraftId,
            java.util.Optional<OtpProof> current, Instant now) {
        if (current.isPresent() && current.get().failureCount() >= maxFailures) {
            throw new ApplicationException(AuthErrorCode.OTP_LOCKED);
        }
    }

    public void validateProof(OtpProof proof, UUID transferDraftId, Instant now) {
        if (proof == null || !proof.transferDraftId().equals(transferDraftId)) {
            throw new ApplicationException(AuthErrorCode.OTP_INVALID);
        }
        if (proof.consumedAt() != null) {
            throw new ApplicationException(AuthErrorCode.OTP_INVALID);
        }
        if (!proof.expiresAt().isAfter(now)) {
            throw new ApplicationException(AuthErrorCode.OTP_INVALID);
        }
        if (proof.failureCount() >= maxFailures) {
            throw new ApplicationException(AuthErrorCode.OTP_LOCKED);
        }
    }
}

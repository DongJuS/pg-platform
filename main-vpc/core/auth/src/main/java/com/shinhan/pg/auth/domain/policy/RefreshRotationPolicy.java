package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.auth.domain.model.RefreshSession;
import com.shinhan.pg.common.commonerror.ApplicationException;

import java.time.Instant;

/**
 * [MVP1-F03] refresh 회전·재사용 탐지 규칙. 재사용 감지 시 호출자가 family 전체를 철회한다.
 * 기본 로그아웃 보완(MVP1-F18)은 보류.
 */
public final class RefreshRotationPolicy {
    public void validateRotation(RefreshSession current, Instant now) {
        if (current == null) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        if (current.revokedAt() != null) {
            throw new ApplicationException(AuthErrorCode.TOKEN_REUSED);
        }
        if (!current.expiresAt().isAfter(now)) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}

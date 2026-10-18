package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.common.commonerror.ApplicationException;

/**
 * [MVP1-F01] 이메일/비밀번호 입력 검증. 합의 수치 초안: 10자 이상, 영문·숫자 포함,
 * 이메일 아이디 부분과 4자 이상 동일한 문자열 포함 금지. 정규화·복잡도 수치 변경은 계약 확정 후 조정한다.
 * 해시는 별도 PasswordHashPort를 사용한다.
 */
public final class PasswordPolicy {
    public void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 10 || rawPassword.length() > 128) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        var hasLetter = rawPassword.chars().anyMatch(Character::isLetter);
        var hasDigit = rawPassword.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    public void validateNotDerivedFromEmail(String rawPassword, String email) {
        if (rawPassword == null || email == null) {
            return;
        }
        var localPart = email.split("@", 2)[0];
        if (localPart.length() >= 4 && rawPassword.toLowerCase().contains(localPart.toLowerCase())) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }
}

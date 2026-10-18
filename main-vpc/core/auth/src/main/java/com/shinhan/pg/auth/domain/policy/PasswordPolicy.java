package com.shinhan.pg.auth.domain.policy;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP1-F01] 이메일/비밀번호 입력 검증 위치. 비밀번호 정책 수치·정규화 규칙은 미정. 해시는 별도 PasswordHashPort를 사용한다.
 * TODO: 이 파일에 합의된 규칙과 불변식만 구현한다. Spring·JPA·Kafka·HTTP에 의존하지 않는다.
 * </pre>
 */
public final class PasswordPolicy {
    public void validatePassword(String rawPassword) {
        throw new FeatureNotImplementedException("MVP1-F01");
    }
}

package com.shinhan.pg.account.adapter.in.web;

import com.shinhan.pg.account.domain.error.AccountErrorCode;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * TODO: AccountErrorCode를 메서드 주석의 400/401/403/404/409/422/429/5xx 계약과 연결한다.
 * 업무 오류를 성공 DTO로 바꾸지 않는다. 현재 미구현 endpoint는 공통 handler가 501로 표시한다.
 * </pre>
 */
public final class AccountErrorMapper {
    public org.springframework.http.HttpStatus status(AccountErrorCode error) {
        throw new FeatureNotImplementedException("account-ERROR-CONTRACT");
    }
}

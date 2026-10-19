package com.shinhan.pg.account.adapter.in.web;

import com.shinhan.pg.account.domain.error.AccountErrorCode;

/**
 * <pre>
 * TODO: AccountErrorCode를 메서드 주석의 400/401/403/404/409/422/429/5xx 계약과 연결한다.
 * 업무 오류를 성공 DTO로 바꾸지 않는다. 현재 미구현 endpoint는 공통 handler가 501로 표시한다.
 * </pre>
 *
 * <p>[MVP1-F07..F10] AccountErrorCode → HTTP 계약 매핑. 실제 변환은 공통 PlatformExceptionHandler가
 * ErrorType으로 수행하며 이 매퍼는 상태 계약을 문서화한다(auth의 AuthErrorMapper와 동일 패턴).
 * 업무 오류를 성공 DTO로 바꾸지 않는다.
 */
public final class AccountErrorMapper {
    public org.springframework.http.HttpStatus status(AccountErrorCode error) {
        return switch (error.type()) {
            case INVALID -> org.springframework.http.HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> org.springframework.http.HttpStatus.NOT_FOUND;
            case CONFLICT -> org.springframework.http.HttpStatus.CONFLICT;
            case UNAUTHORIZED -> org.springframework.http.HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> org.springframework.http.HttpStatus.FORBIDDEN;
            case EXTERNAL_FAILURE -> org.springframework.http.HttpStatus.BAD_GATEWAY;
            case INTERNAL -> org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private AccountErrorMapper() {}
}

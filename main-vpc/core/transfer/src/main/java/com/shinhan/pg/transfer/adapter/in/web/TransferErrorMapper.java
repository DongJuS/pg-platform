package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.transfer.domain.error.TransferErrorCode;

/**
 * <pre>
 * TransferErrorCode를 메서드 주석의 400/401/403/404/409/422 계약과 연결한다.
 * 업무 오류를 성공 DTO로 바꾸지 않는다. 미구현 endpoint(해외 송금)는 공통 handler가 501로 표시한다.
 * </pre>
 *
 * <p>WO-A6 실구현: 409 중복/상태 충돌, 403 타인 소유, 404 부재, 422 원장 거절(잔액 부족),
 * 401 인증 문맥 부재, 400 입력 오류. RESULT_UNKNOWN은 HTTP 오류로 변환하지 않고
 * 이체 상태 UNKNOWN으로 표현하는 것이 원칙이다 — 여기까지 도달하는 경우는 내부 계약 위반이므로 500.</p>
 */
public final class TransferErrorMapper {
    public org.springframework.http.HttpStatus status(TransferErrorCode error) {
        return switch (error) {
            case DUPLICATE_REQUEST -> org.springframework.http.HttpStatus.CONFLICT;
            case INVALID_STATE -> org.springframework.http.HttpStatus.CONFLICT;
            case ACCOUNT_FORBIDDEN -> org.springframework.http.HttpStatus.FORBIDDEN;
            case NOT_FOUND -> org.springframework.http.HttpStatus.NOT_FOUND;
            case INSUFFICIENT_FUNDS -> org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
            case UNAUTHENTICATED -> org.springframework.http.HttpStatus.UNAUTHORIZED;
            case INVALID_REQUEST -> org.springframework.http.HttpStatus.BAD_REQUEST;
            case OTP_INVALID -> org.springframework.http.HttpStatus.UNAUTHORIZED;
            case ACCOUNT_INACTIVE -> org.springframework.http.HttpStatus.CONFLICT;
            case AUTH_UNAVAILABLE -> org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
            case RESULT_UNKNOWN -> org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /** ErrorResponse.code에 실릴 안정적 식별자. */
    public String code(TransferErrorCode error) {
        return "TRANSFER-" + error.name().replace('_', '-');
    }
}

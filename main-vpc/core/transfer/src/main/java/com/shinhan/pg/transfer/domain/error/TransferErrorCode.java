package com.shinhan.pg.transfer.domain.error;

/** 업무 오류 식별자 초안. HTTP 상태·표시 문구는 웹 계층에서 최종 계약한다. */
public enum TransferErrorCode {
    DUPLICATE_REQUEST,
    ACCOUNT_FORBIDDEN,
    INSUFFICIENT_FUNDS,
    RESULT_UNKNOWN,
    // WO-A6 실구현에서 추가: 조회 부재(404), 최종 상태 역전 등 불허 전이(409), 인증 문맥 부재(401), 입력 오류(400).
    NOT_FOUND,
    INVALID_STATE,
    UNAUTHENTICATED,
    INVALID_REQUEST,
    OTP_INVALID,
    ACCOUNT_INACTIVE,
    AUTH_UNAVAILABLE
}

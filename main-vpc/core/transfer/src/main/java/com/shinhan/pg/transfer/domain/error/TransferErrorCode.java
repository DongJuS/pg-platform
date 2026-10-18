package com.shinhan.pg.transfer.domain.error;

/** 업무 오류 식별자 초안. HTTP 상태·표시 문구는 웹 계층에서 최종 계약한다. */
public enum TransferErrorCode {
    DUPLICATE_REQUEST,
    ACCOUNT_FORBIDDEN,
    INSUFFICIENT_FUNDS,
    RESULT_UNKNOWN
}

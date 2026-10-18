package com.shinhan.pg.ledger.domain.error;

/** 업무 오류 식별자 초안. HTTP 상태·표시 문구는 웹 계층에서 최종 계약한다. */
public enum LedgerErrorCode {
    UNBALANCED_JOURNAL,
    INSUFFICIENT_FUNDS,
    DUPLICATE_TRANSACTION,
    TRANSACTION_UNKNOWN
}

package com.shinhan.pg.ledger.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** LedgerErrorCode → HTTP 상태 계약. 422/404는 메서드 주석의 오류 후보와 일치해야 한다. */
class LedgerErrorMapperTest {
    private final LedgerErrorMapper mapper = new LedgerErrorMapper();

    @Test
    void statusMapping() {
        assertEquals(
                HttpStatus.UNPROCESSABLE_ENTITY, mapper.status(LedgerErrorCode.UNBALANCED_JOURNAL));
        assertEquals(
                HttpStatus.UNPROCESSABLE_ENTITY, mapper.status(LedgerErrorCode.INSUFFICIENT_FUNDS));
        assertEquals(HttpStatus.NOT_FOUND, mapper.status(LedgerErrorCode.TRANSACTION_UNKNOWN));
        // 409는 멱등 200 채택으로 현재 미사용. 계약 미정 주석(Controller/Mapper) 참조.
        assertEquals(HttpStatus.CONFLICT, mapper.status(LedgerErrorCode.DUPLICATE_TRANSACTION));
    }
}

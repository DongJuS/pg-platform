package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.ledger.domain.error.LedgerErrorCode;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * <pre>
 * TODO: LedgerErrorCode를 메서드 주석의 400/401/403/404/409/422/429/5xx 계약과 연결한다.
 * 업무 오류를 성공 DTO로 바꾸지 않는다. 현재 미구현 endpoint는 공통 handler가 501로 표시한다.
 * </pre>
 *
 * <p>현재 매핑(F11~F14 구현분): 합계≠0·잔액 부족은 422, 미반영 조회는 404.
 * DUPLICATE_TRANSACTION→409는 "원장 동일 transactionId 재조회와 클라이언트 중복 이체 409는
 * 서로 다른 계약"(미정) 메모에 따라 아직 사용하지 않는다 — 중복 transactionId는 멱등 200으로
 * 기존 journal을 반환한다. 최종 형식 확정 시 이 매핑과 InternalLedgerController 주석을 함께 갱신한다.
 */
public final class LedgerErrorMapper {
    public HttpStatus status(LedgerErrorCode error) {
        return switch (Objects.requireNonNull(error)) {
            case UNBALANCED_JOURNAL -> HttpStatus.UNPROCESSABLE_ENTITY;
            case INSUFFICIENT_FUNDS -> HttpStatus.UNPROCESSABLE_ENTITY;
            case DUPLICATE_TRANSACTION -> HttpStatus.CONFLICT;
            case TRANSACTION_UNKNOWN -> HttpStatus.NOT_FOUND;
        };
    }
}

package com.shinhan.pg.payment.application.port.out;

import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;

import java.util.UUID;

/** 금융 대외 요청은 내부 REST → FEP. 반환 ID는 접수 식별자이며 최종 승인과 구분한다. */
// TODO: 승인 외 취소·부분 환불·결과 조회의 client 메서드/DTO와 idempotency 범위를 FEP PG 후보 Controller의 상세 계약 확정 후 추가한다.
public interface FepPaymentPort {
    UUID requestApproval(ApprovePaymentCommand command);
}

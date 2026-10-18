package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.ApprovePaymentCommand;
import com.shinhan.pg.payment.application.result.ApprovePaymentResult;

/**
 * <pre>
 * [MVP2-F01] 모의 결제
 * API 초안: POST /api/v1/payments
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: Idempotency-Key, 주문번호, 금액
 * 출력 근거: paymentId, 상태
 * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 중복, 502 PG 오류
 * TODO: Payment → FEP 내부 REST → Mock PG. 직접 Mock PG를 호출하지 않는다. 접수와 최종 승인을 구분한다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981c9b971efeb78107e6d
 * </pre>
 */
public interface ApprovePaymentUseCase {
    ApprovePaymentResult approvePayment(ApprovePaymentCommand command);
}

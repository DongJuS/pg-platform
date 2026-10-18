package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.RequestRefundCommand;
import com.shinhan.pg.payment.application.result.RequestRefundResult;

/**
 * <pre>
 * [MVP2-F04] 환불 요청
 * API 초안: POST /api/v1/payments/{paymentId}/refunds
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: 금액, Idempotency-Key
 * 출력 근거: refundId, 상태
 * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 422 한도 초과
 * TODO: 확정 환불액과 처리 중 예약액을 같은 DB 트랜잭션으로 검사한다. 누적 환불 승인액 초과를 방지한다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49815fb6b4c3b5663a8c92
 * </pre>
 */
public interface RequestRefundUseCase {
    RequestRefundResult requestRefund(RequestRefundCommand command);
}

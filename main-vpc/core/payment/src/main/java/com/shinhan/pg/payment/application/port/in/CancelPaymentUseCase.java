package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.CancelPaymentCommand;
import com.shinhan.pg.payment.application.result.CancelPaymentResult;

/**
 * <pre>
 * [MVP2-F03] 결제 취소
 * API 초안: POST /api/v1/payments/{paymentId}/cancel
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: paymentId
 * 출력 근거: 상태
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 취소 불가 상태
 * TODO: 진행 중 환불 예약과 취소가 충돌할 때의 우선순위·해제 조건은 미정이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a498145b515caf3f1cb101e
 * </pre>
 */
public interface CancelPaymentUseCase {
    CancelPaymentResult cancelPayment(CancelPaymentCommand command);
}

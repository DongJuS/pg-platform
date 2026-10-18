package com.shinhan.pg.payment.application.port.in;

import com.shinhan.pg.payment.application.command.GetPaymentCommand;
import com.shinhan.pg.payment.application.result.GetPaymentResult;

/**
 * <pre>
 * [MVP2-F02] 결제 조회
 * API 초안: GET /api/v1/payments/{paymentId}
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: paymentId
 * 출력 근거: 상태, 금액
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 403, 404
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981449d22d4d2771b461e
 * </pre>
 */
public interface GetPaymentUseCase {
    GetPaymentResult getPayment(GetPaymentCommand command);
}

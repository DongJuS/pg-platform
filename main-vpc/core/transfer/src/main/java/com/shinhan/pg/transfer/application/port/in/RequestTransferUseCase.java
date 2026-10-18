package com.shinhan.pg.transfer.application.port.in;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.result.RequestTransferResult;

/**
 * <pre>
 * [MVP1-F15] 가상 이체
 * API 초안: POST /api/v1/transfers
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: Idempotency-Key 헤더, 출금·입금 계좌, 금액, otpToken
 * 출력 근거: transferId, 상태; 중복 요청은 오류
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 중복 요청·키 충돌, 422 잔액 부족, 401 OTP
 * TODO: actorUserId + idempotencyKey 고유 조건과 요청 해시를 예약한다. 같은 키 재요청은 사용자 결정대로 409 오류이며 신규 이체를 만들지 않는다.
 * TODO: 원장 REST 성공과 ledger.posted 소비는 동일 완료 경로로 수렴시킨다. 시간 초과는 UNKNOWN으로 두고 동일 transactionId로 조회한다.
 * TODO: 성공 200은 동기 완료의 응답 초안이다. 접수/결과불명 시 HTTP 응답 계약은 미정이며 코드로 성공을 꾸미지 않는다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981ae8beff34a15fd47f1
 * </pre>
 */
public interface RequestTransferUseCase {
    RequestTransferResult requestTransfer(RequestTransferCommand command);
}

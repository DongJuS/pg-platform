package com.shinhan.pg.ledger.application.port.in;

import com.shinhan.pg.ledger.application.command.GetBalanceCommand;
import com.shinhan.pg.ledger.application.result.GetBalanceResult;

/**
 * <pre>
 * [MVP1-F11] 잔액 조회
 * API 초안: GET /api/v1/accounts/{accountId}/balances
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: accountId
 * 출력 근거: 잔액, 기준 시각
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 403
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981b9b6c9fc7a3de22119
 * </pre>
 */
public interface GetBalanceUseCase {
    GetBalanceResult getBalance(GetBalanceCommand command);
}

package com.shinhan.pg.account.application.port.in;

import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.result.OpenAccountResult;

/**
 * <pre>
 * [MVP1-F07] 가상 계좌 개설
 * API 초안: POST /api/v1/accounts
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: 통화(KRW), 계좌 별칭
 * 출력 근거: accountId, 계좌번호
 * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 400
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49812bb64ff9c7f4ac5c09
 * </pre>
 */
public interface OpenAccountUseCase {
    OpenAccountResult openAccount(OpenAccountCommand command);
}

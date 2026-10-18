package com.shinhan.pg.account.application.port.in;

import com.shinhan.pg.account.application.command.GetAccountCommand;
import com.shinhan.pg.account.application.result.GetAccountResult;

/**
 * <pre>
 * [MVP1-F09] 계좌 상세
 * API 초안: GET /api/v1/accounts/{accountId}
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: accountId
 * 출력 근거: 계좌 메타데이터
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 403 타인 계좌, 404
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a498153b3bff10d8ad2b396
 * </pre>
 */
public interface GetAccountUseCase {
    GetAccountResult getAccount(GetAccountCommand command);
}

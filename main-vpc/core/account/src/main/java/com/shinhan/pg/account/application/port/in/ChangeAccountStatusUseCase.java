package com.shinhan.pg.account.application.port.in;

import com.shinhan.pg.account.application.command.ChangeAccountStatusCommand;
import com.shinhan.pg.account.application.result.ChangeAccountStatusResult;

/**
 * <pre>
 * [MVP1-F10] 계좌 상태 변경
 * API 초안: PATCH /api/v1/accounts/{accountId}/status
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 서비스 인증 / INTERNAL
 * 입력 근거: status, 사유
 * 출력 근거: 변경된 상태
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 허용되지 않는 전이
 * TODO: 경로에 /api/v1이 있어도 내부 서비스 인증 대상이다. Gateway 외부 라우팅에서 제외할 조건을 적어둔다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49813aa4d2f0ad91e9efac
 * </pre>
 */
public interface ChangeAccountStatusUseCase {
    ChangeAccountStatusResult changeAccountStatus(ChangeAccountStatusCommand command);
}

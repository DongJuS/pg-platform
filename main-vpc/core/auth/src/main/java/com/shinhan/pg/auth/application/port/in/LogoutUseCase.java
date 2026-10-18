package com.shinhan.pg.auth.application.port.in;

import com.shinhan.pg.auth.application.command.LogoutCommand;

/**
 * <pre>
 * [MVP1-F04] 로그아웃
 * API 초안: POST /api/v1/auth/logout
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: accessToken
 * 출력 근거: 204
 * 정상 응답 후보: 204 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: —
 * TODO: 기본 로그아웃은 access jti 철회다. refresh 세션 전체 철회·전체 기기 로그아웃(MVP1-F18)은 보류하며 이 메서드에 자동 추가하지 않는다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a49816c96c5d41e6ffbcb52
 * </pre>
 */
public interface LogoutUseCase {
    void logout(LogoutCommand command);
}

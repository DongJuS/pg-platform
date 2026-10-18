package com.shinhan.pg.auth.application.port.in;

import com.shinhan.pg.auth.application.command.RefreshTokenCommand;
import com.shinhan.pg.auth.application.result.RefreshTokenResult;

/**
 * <pre>
 * [MVP1-F03] 토큰 재발급
 * API 초안: POST /api/v1/auth/refresh
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 없음 / PUBLIC
 * 입력 근거: refreshToken
 * 출력 근거: 새 accessToken, refreshToken
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 401 만료·철회 토큰
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a498100bb77d2811deb087f
 * </pre>
 */
public interface RefreshTokenUseCase {
    RefreshTokenResult refreshToken(RefreshTokenCommand command);
}

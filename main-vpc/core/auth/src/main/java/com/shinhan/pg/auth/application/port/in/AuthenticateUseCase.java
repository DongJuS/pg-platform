package com.shinhan.pg.auth.application.port.in;

import com.shinhan.pg.auth.application.command.AuthenticateCommand;
import com.shinhan.pg.auth.application.result.AuthenticateResult;

/**
 * <pre>
 * [MVP1-F02] 로그인
 * API 초안: POST /api/v1/auth/login
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 없음 / PUBLIC
 * 입력 근거: 이메일, 비밀번호
 * 출력 근거: accessToken, refreshToken, 만료 시각
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 401 자격증명 오류, 429 요청 과다
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981c69805e570fa72cf1b
 * </pre>
 */
public interface AuthenticateUseCase {
    AuthenticateResult authenticate(AuthenticateCommand command);
}

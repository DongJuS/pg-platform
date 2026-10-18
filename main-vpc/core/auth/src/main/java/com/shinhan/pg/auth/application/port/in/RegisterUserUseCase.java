package com.shinhan.pg.auth.application.port.in;

import com.shinhan.pg.auth.application.command.RegisterUserCommand;
import com.shinhan.pg.auth.application.result.RegisterUserResult;

/**
 * <pre>
 * [MVP1-F01] 회원가입
 * API 초안: POST /api/v1/auth/register
 * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 없음 / PUBLIC
 * 입력 근거: 이메일, 비밀번호, 이름, 전화번호
 * 출력 근거: userId
 * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 409 이메일 중복, 400 비밀번호 규칙 위반
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://app.notion.com/p/3faad9292a4981ee85fdfeca462ac388
 * </pre>
 */
public interface RegisterUserUseCase {
    RegisterUserResult registerUser(RegisterUserCommand command);
}

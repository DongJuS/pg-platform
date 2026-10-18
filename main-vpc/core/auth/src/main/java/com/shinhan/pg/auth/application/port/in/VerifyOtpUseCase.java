package com.shinhan.pg.auth.application.port.in;

import com.shinhan.pg.auth.application.command.VerifyOtpCommand;
import com.shinhan.pg.auth.application.result.VerifyOtpResult;

/**
 * <pre>
 * [MVP1-F06] 이체 OTP 확인
 * API 초안: POST /api/v1/auth/otp/verify
 * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
 * 인증/접근: 사용자 JWT / PUBLIC
 * 입력 근거: transferDraftId, otpCode
 * 출력 근거: otpToken(1회용)
 * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
 * 오류 후보: 401 불일치, 423 잠금
 * TODO: 실제 OTP 제공사 연동이 아니라 Mock OTP다. proof의 유효시간·소모 조건은 후속 계약이다.
 * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
 * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
 * </pre>
 */
public interface VerifyOtpUseCase {
    VerifyOtpResult verifyOtp(VerifyOtpCommand command);
}

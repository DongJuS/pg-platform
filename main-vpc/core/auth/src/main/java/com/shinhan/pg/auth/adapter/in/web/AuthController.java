package com.shinhan.pg.auth.adapter.in.web;

import com.shinhan.pg.auth.adapter.in.web.request.LoginRequest;
import com.shinhan.pg.auth.adapter.in.web.request.RefreshTokenRequest;
import com.shinhan.pg.auth.adapter.in.web.request.RegisterUserRequest;
import com.shinhan.pg.auth.adapter.in.web.request.VerifyOtpRequest;
import com.shinhan.pg.auth.adapter.in.web.response.LoginResponse;
import com.shinhan.pg.auth.adapter.in.web.response.RefreshTokenResponse;
import com.shinhan.pg.auth.adapter.in.web.response.RegisterUserResponse;
import com.shinhan.pg.auth.adapter.in.web.response.VerifyOtpResponse;
import com.shinhan.pg.auth.application.command.AuthenticateCommand;
import com.shinhan.pg.auth.application.command.LogoutCommand;
import com.shinhan.pg.auth.application.command.RefreshTokenCommand;
import com.shinhan.pg.auth.application.command.RegisterUserCommand;
import com.shinhan.pg.auth.application.command.VerifyOtpCommand;
import com.shinhan.pg.auth.application.port.in.AuthenticateUseCase;
import com.shinhan.pg.auth.application.port.in.LogoutUseCase;
import com.shinhan.pg.auth.application.port.in.RefreshTokenUseCase;
import com.shinhan.pg.auth.application.port.in.RegisterUserUseCase;
import com.shinhan.pg.auth.application.port.in.VerifyOtpUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
@RestController
@Profile("skeleton")
public final class AuthController {
    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUseCase authenticateUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            AuthenticateUseCase authenticateUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            VerifyOtpUseCase verifyOtpUseCase) {
        this.registerUserUseCase = java.util.Objects.requireNonNull(registerUserUseCase);
        this.authenticateUseCase = java.util.Objects.requireNonNull(authenticateUseCase);
        this.refreshTokenUseCase = java.util.Objects.requireNonNull(refreshTokenUseCase);
        this.logoutUseCase = java.util.Objects.requireNonNull(logoutUseCase);
        this.verifyOtpUseCase = java.util.Objects.requireNonNull(verifyOtpUseCase);
    }

    /**
     * <pre>
     * [MVP1-F01] 회원가입
     * API 초안: POST /api/v1/auth/register
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 없음 / PUBLIC
     * 입력 근거: 이메일, 비밀번호, 이름, 전화번호
     * 출력 근거: userId
     * 정상 응답 후보: 201
     * 오류 후보: 409 이메일 중복, 400 비밀번호 규칙 위반
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ee85fdfeca462ac388
     * </pre>
     */
    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(@RequestBody RegisterUserRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                registerUserUseCase.registerUser(
                        new RegisterUserCommand(
                                UUID.randomUUID(),
                                request.email(),
                                request.password(),
                                request.name(),
                                request.phoneNumber()));
        return new RegisterUserResponse(result.userId());
    }

    /**
     * <pre>
     * [MVP1-F02] 로그인
     * API 초안: POST /api/v1/auth/login
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 없음 / PUBLIC
     * 입력 근거: 이메일, 비밀번호
     * 출력 근거: accessToken, refreshToken, 만료 시각
     * 정상 응답 후보: 200
     * 오류 후보: 401 자격증명 오류, 429 요청 과다
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981c69805e570fa72cf1b
     * </pre>
     */
    @PostMapping("/api/v1/auth/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody LoginRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                authenticateUseCase.authenticate(
                        new AuthenticateCommand(
                                UUID.randomUUID(), request.email(), request.password()));
        return new LoginResponse(
                result.accessToken(),
                result.refreshToken(),
                result.accessExpiresAt(),
                result.refreshExpiresAt());
    }

    /**
     * <pre>
     * [MVP1-F03] 토큰 재발급
     * API 초안: POST /api/v1/auth/refresh
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 없음 / PUBLIC
     * 입력 근거: refreshToken
     * 출력 근거: 새 accessToken, refreshToken
     * 정상 응답 후보: 200
     * 오류 후보: 401 만료·철회 토큰
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498100bb77d2811deb087f
     * </pre>
     */
    @PostMapping("/api/v1/auth/refresh")
    @ResponseStatus(HttpStatus.OK)
    public RefreshTokenResponse refreshToken(@RequestBody RefreshTokenRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                refreshTokenUseCase.refreshToken(
                        new RefreshTokenCommand(UUID.randomUUID(), request.refreshToken()));
        return new RefreshTokenResponse(
                result.accessToken(),
                result.refreshToken(),
                result.accessExpiresAt(),
                result.refreshExpiresAt());
    }

    /**
     * <pre>
     * [MVP1-F04] 로그아웃
     * API 초안: POST /api/v1/auth/logout
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: accessToken
     * 출력 근거: 204
     * 정상 응답 후보: 204
     * 오류 후보: —
     * TODO: 기본 로그아웃은 access jti 철회다. refresh 세션 전체 철회·전체 기기 로그아웃(MVP1-F18)은 보류하며 이 메서드에 자동 추가하지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49816c96c5d41e6ffbcb52
     * </pre>
     */
    @PostMapping("/api/v1/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestHeader("Authorization") String authorization) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        logoutUseCase.logout(new LogoutCommand(UUID.randomUUID(), actorUserId, authorization));
    }

    /**
     * <pre>
     * [MVP1-F06] 이체 OTP 확인
     * API 초안: POST /api/v1/auth/otp/verify
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: transferDraftId, otpCode
     * 출력 근거: otpToken(1회용)
     * 정상 응답 후보: 200
     * 오류 후보: 401 불일치, 423 잠금
     * TODO: 실제 OTP 제공사 연동이 아니라 Mock OTP다. proof의 유효시간·소모 조건은 후속 계약이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @PostMapping("/api/v1/auth/otp/verify")
    @ResponseStatus(HttpStatus.OK)
    public VerifyOtpResponse verifyOtp(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody VerifyOtpRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                verifyOtpUseCase.verifyOtp(
                        new VerifyOtpCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.transferDraftId(),
                                request.otpCode()));
        return new VerifyOtpResponse(result.otpToken(), result.expiresAt());
    }
}

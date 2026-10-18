package com.shinhan.pg.auth.application.service;

import com.shinhan.pg.auth.domain.security.CryptoSupport;
import com.shinhan.pg.auth.domain.security.TotpMock;
import com.shinhan.pg.auth.application.command.AuthenticateCommand;
import com.shinhan.pg.auth.application.command.GetCurrentUserCommand;
import com.shinhan.pg.auth.application.command.LogoutCommand;
import com.shinhan.pg.auth.application.command.RefreshTokenCommand;
import com.shinhan.pg.auth.application.command.RegisterUserCommand;
import com.shinhan.pg.auth.application.command.VerifyOtpCommand;
import com.shinhan.pg.auth.application.port.in.AuthenticateUseCase;
import com.shinhan.pg.auth.application.port.in.GetCurrentUserUseCase;
import com.shinhan.pg.auth.application.port.in.LogoutUseCase;
import com.shinhan.pg.auth.application.port.in.RefreshTokenUseCase;
import com.shinhan.pg.auth.application.port.in.RegisterUserUseCase;
import com.shinhan.pg.auth.application.port.in.VerifyOtpUseCase;
import com.shinhan.pg.auth.application.port.out.OtpProofRepository;
import com.shinhan.pg.auth.application.port.out.PasswordHashPort;
import com.shinhan.pg.auth.application.port.out.RefreshSessionRepository;
import com.shinhan.pg.auth.application.port.out.RevokedTokenStorePort;
import com.shinhan.pg.auth.application.port.out.TokenIssuancePort;
import com.shinhan.pg.auth.application.port.out.UserRepository;
import com.shinhan.pg.auth.application.result.AuthenticateResult;
import com.shinhan.pg.auth.application.result.GetCurrentUserResult;
import com.shinhan.pg.auth.application.result.RefreshTokenResult;
import com.shinhan.pg.auth.application.result.RegisterUserResult;
import com.shinhan.pg.auth.application.result.VerifyOtpResult;
import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.auth.domain.model.OtpProof;
import com.shinhan.pg.auth.domain.model.RefreshSession;
import com.shinhan.pg.auth.domain.model.User;
import com.shinhan.pg.auth.domain.policy.OtpVerificationPolicy;
import com.shinhan.pg.auth.domain.policy.PasswordPolicy;
import com.shinhan.pg.auth.domain.policy.RefreshRotationPolicy;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * [MVP1-F01..F06] 인증 업무 구현. Controller는 이 클래스가 구현하는 input port만 호출한다.
 * access 토큰은 RS256 JWT, refresh는 불투명 토큰(SHA-256 해시 저장)이며 회전·재사용 탐지를 적용한다.
 * 로그인 rate limit의 Redis 카운터는 후속 계약으로 남긴다(재시도 억제는 OTP 잠금으로 우선 적용).
 */
public class AuthApplicationService
        implements AuthenticateUseCase,
                RegisterUserUseCase,
                RefreshTokenUseCase,
                LogoutUseCase,
                GetCurrentUserUseCase,
                VerifyOtpUseCase {
    private final PasswordHashPort passwordHashPort;
    private final TokenIssuancePort tokenIssuancePort;
    private final RevokedTokenStorePort revokedTokenStorePort;
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final OtpProofRepository otpProofRepository;
    private final PasswordPolicy passwordPolicy;
    private final RefreshRotationPolicy refreshRotationPolicy;
    private final OtpVerificationPolicy otpVerificationPolicy;
    private final byte[] otpServerSecret;
    private final Duration refreshTokenTtl;
    private final Duration otpTokenTtl;

    public AuthApplicationService(
            PasswordHashPort passwordHashPort,
            TokenIssuancePort tokenIssuancePort,
            RevokedTokenStorePort revokedTokenStorePort,
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            OtpProofRepository otpProofRepository,
            byte[] otpServerSecret,
            Duration refreshTokenTtl,
            Duration otpTokenTtl) {
        this.passwordHashPort = java.util.Objects.requireNonNull(passwordHashPort);
        this.tokenIssuancePort = java.util.Objects.requireNonNull(tokenIssuancePort);
        this.revokedTokenStorePort = java.util.Objects.requireNonNull(revokedTokenStorePort);
        this.userRepository = java.util.Objects.requireNonNull(userRepository);
        this.refreshSessionRepository = java.util.Objects.requireNonNull(refreshSessionRepository);
        this.otpProofRepository = java.util.Objects.requireNonNull(otpProofRepository);
        this.passwordPolicy = new PasswordPolicy();
        this.refreshRotationPolicy = new RefreshRotationPolicy();
        this.otpVerificationPolicy = new OtpVerificationPolicy();
        this.otpServerSecret = otpServerSecret.clone();
        this.refreshTokenTtl = java.util.Objects.requireNonNull(refreshTokenTtl);
        this.otpTokenTtl = java.util.Objects.requireNonNull(otpTokenTtl);
    }

    /**
     * <pre>
     * [MVP1-F01] 회원가입. 이메일 고유 + 비밀번호 정책 검사 후 PBKDF2 해시 저장.
     * 오류: 409 EMAIL_ALREADY_EXISTS, 401 INVALID_CREDENTIALS(비밀번호 규칙 위반 — TODO[CONTRACT] 400 계약 후보).
     * </pre>
     */
    @Override
    @Transactional
    public RegisterUserResult registerUser(RegisterUserCommand command) {
        passwordPolicy.validatePassword(command.password());
        passwordPolicy.validateNotDerivedFromEmail(command.password(), command.email());
        userRepository
                .findByEmail(command.email())
                .ifPresent(
                        existing -> {
                            throw new ApplicationException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
                        });
        var user =
                new User(
                        UUID.randomUUID(),
                        command.email(),
                        passwordHashPort.hash(command.password()),
                        command.name(),
                        command.phoneNumber(),
                        List.of("USER"));
        userRepository.save(user);
        return new RegisterUserResult(user.id());
    }

    /**
     * <pre>
     * [MVP1-F02] 로그인. 자격증명 확인 후 access JWT 발급 + refresh 세션 생성(새 family).
     * 오류: 401 INVALID_CREDENTIALS.
     * </pre>
     */
    @Override
    @Transactional
    public AuthenticateResult authenticate(AuthenticateCommand command) {
        var user =
                userRepository
                        .findByEmail(command.email())
                        .orElseThrow(
                                () -> new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS));
        if (!passwordHashPort.matches(command.password(), user.passwordHash())) {
            throw new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        var access = tokenIssuancePort.issueAccessToken(user.id());
        var refresh = issueRefreshSession(user.id(), UUID.randomUUID());
        return new AuthenticateResult(
                access.token(), refresh.token(), access.expiresAt(), refresh.expiresAt());
    }

    /**
     * <pre>
     * [MVP1-F03] 토큰 재발급. refresh 회전 + 재사용 감지 시 family 전체 철회.
     * 오류: 401 INVALID_CREDENTIALS, 409 TOKEN_REUSED.
     * </pre>
     */
    @Override
    @Transactional
    public RefreshTokenResult refreshToken(RefreshTokenCommand command) {
        var now = Instant.now();
        var session =
                refreshSessionRepository
                        .findByTokenHash(CryptoSupport.sha256Hex(command.refreshToken()))
                        .orElseThrow(
                                () -> new ApplicationException(AuthErrorCode.INVALID_CREDENTIALS));
        if (session.revokedAt() != null) {
            refreshSessionRepository.revokeFamily(session.familyId(), now);
            throw new ApplicationException(AuthErrorCode.TOKEN_REUSED);
        }
        refreshRotationPolicy.validateRotation(session, now);
        refreshSessionRepository.save(revoked(session, now));
        var access = tokenIssuancePort.issueAccessToken(session.userId());
        var rotated = issueRefreshSession(session.userId(), session.familyId());
        return new RefreshTokenResult(
                access.token(), rotated.token(), access.expiresAt(), rotated.expiresAt());
    }

    /**
     * <pre>
     * [MVP1-F04] 로그아웃. access jti를 남은 만료 시간만큼 Redis 철회 목록에 저장한다.
     * 서명이 유효하지 않은 토큰은 철회할 수 없으므로 멱등하게 성공 처리한다(계약 초안).
     * </pre>
     */
    @Override
    public void logout(LogoutCommand command) {
        if (command.authorization() == null || !command.authorization().startsWith("Bearer ")) {
            return;
        }
        try {
            var verified =
                    tokenIssuancePort.verifyAccessToken(
                            command.authorization().substring("Bearer ".length()));
            revokedTokenStorePort.revokeUntilExpiry(verified.jti(), verified.expiresAt());
        } catch (RuntimeException invalidToken) {
            // TODO[CONTRACT]: 유효하지 않은 토큰의 로그아웃 응답(204 vs 401)은 최종 계약 후 조정한다.
        }
    }

    /** [MVP1-F05] 내 정보 조회. 오류: 404 USER_NOT_FOUND. */
    @Override
    @Transactional(readOnly = true)
    public GetCurrentUserResult getCurrentUser(GetCurrentUserCommand command) {
        var user =
                userRepository
                        .findById(command.actorUserId())
                        .orElseThrow(
                                () -> new ApplicationException(AuthErrorCode.USER_NOT_FOUND));
        return new GetCurrentUserResult(user.id(), user.name(), user.roles());
    }

    /**
     * <pre>
     * [MVP1-F06] 이체 OTP 확인. Mock TOTP(±1 단계) 검증, 실패 시 횟수 누적, 성공 시 일회 소모 기록과
     * otpToken(만료 otpTokenTtl) 발급. 오류: 401 OTP_INVALID, 403 OTP_LOCKED(계약 초안 423 대체 — TODO[CONTRACT]).
     * </pre>
     */
    @Override
    @Transactional
    public VerifyOtpResult verifyOtp(VerifyOtpCommand command) {
        var now = Instant.now();
        var current =
                otpProofRepository.findLatestByUserIdAndTransferDraftId(
                        command.actorUserId(), command.transferDraftId());
        otpVerificationPolicy.ensureNotLocked(command.transferDraftId(), current, now);
        current.ifPresent(
                proof ->
                        otpVerificationPolicy.validateProof(
                                proof, command.transferDraftId(), now));

        var seed = TotpMock.seedFor(otpServerSecret, command.actorUserId());
        if (!TotpMock.verify(seed, command.otpCode(), now, 1)) {
            var failed =
                    current
                            .map(proof -> proof.withFailure(proof.failureCount() + 1))
                            .orElseGet(
                                    () ->
                                            new OtpProof(
                                                    UUID.randomUUID(),
                                                    command.actorUserId(),
                                                    command.transferDraftId(),
                                                    CryptoSupport.sha256Hex(
                                                            "otp-attempt:" + UUID.randomUUID()),
                                                    now.plus(otpTokenTtl),
                                                    null,
                                                    1));
            otpProofRepository.save(failed);
            throw new ApplicationException(AuthErrorCode.OTP_INVALID);
        }

        var expiresAt = now.plus(otpTokenTtl);
        var proof =
                new OtpProof(
                        UUID.randomUUID(),
                        command.actorUserId(),
                        command.transferDraftId(),
                        CryptoSupport.sha256Hex("otp-proof:" + UUID.randomUUID()),
                        expiresAt,
                        now,
                        0);
        otpProofRepository.save(proof);
        return new VerifyOtpResult(otpToken(proof.id(), expiresAt), expiresAt);
    }

    private RefreshTokenSession issueRefreshSession(UUID userId, UUID familyId) {
        var token = CryptoSupport.randomToken();
        var expiresAt = Instant.now().plus(refreshTokenTtl);
        refreshSessionRepository.save(
                new RefreshSession(
                        UUID.randomUUID(),
                        userId,
                        CryptoSupport.sha256Hex(token),
                        familyId,
                        expiresAt,
                        null));
        return new RefreshTokenSession(token, expiresAt);
    }

    private RefreshSession revoked(RefreshSession session, Instant at) {
        return new RefreshSession(
                session.id(),
                session.userId(),
                session.tokenHash(),
                session.familyId(),
                session.expiresAt(),
                at);
    }

    private String otpToken(UUID proofId, Instant expiresAt) {
        var body = proofId + "." + expiresAt.getEpochSecond();
        var encoded =
                java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var signature = CryptoSupport.hmacSha256Hex(otpServerSecret, "otp-token:" + body);
        return "v1." + encoded + "." + signature;
    }

    private record RefreshTokenSession(String token, Instant expiresAt) {}
}

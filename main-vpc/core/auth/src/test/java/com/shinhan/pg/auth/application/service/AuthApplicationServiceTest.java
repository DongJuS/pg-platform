package com.shinhan.pg.auth.application.service;

import com.shinhan.pg.auth.application.command.AuthenticateCommand;
import com.shinhan.pg.auth.application.command.GetCurrentUserCommand;
import com.shinhan.pg.auth.application.command.LogoutCommand;
import com.shinhan.pg.auth.application.command.RefreshTokenCommand;
import com.shinhan.pg.auth.application.command.RegisterUserCommand;
import com.shinhan.pg.auth.application.command.VerifyOtpCommand;
import com.shinhan.pg.auth.application.port.out.OtpProofRepository;
import com.shinhan.pg.auth.application.port.out.PasswordHashPort;
import com.shinhan.pg.auth.application.port.out.RefreshSessionRepository;
import com.shinhan.pg.auth.application.port.out.RevokedTokenStorePort;
import com.shinhan.pg.auth.application.port.out.TokenIssuancePort;
import com.shinhan.pg.auth.application.port.out.UserRepository;
import com.shinhan.pg.auth.adapter.out.security.Pbkdf2PasswordHasher;
import com.shinhan.pg.auth.adapter.out.security.Rs256JwtSupport;
import com.shinhan.pg.auth.domain.error.AuthErrorCode;
import com.shinhan.pg.auth.domain.model.OtpProof;
import com.shinhan.pg.auth.domain.model.RefreshSession;
import com.shinhan.pg.auth.domain.model.User;
import com.shinhan.pg.auth.domain.security.CryptoSupport;
import com.shinhan.pg.auth.domain.security.TotpMock;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 인메모리 페이크 저장소로 인증 흐름 전체를 검증한다. DB 계약은 AuthApiIntegrationTest가 담당한다. */
class AuthApplicationServiceTest {
    private InMemoryUsers users;
    private InMemorySessions sessions;
    private InMemoryProofs proofs;
    private InMemoryRevoked revoked;
    private AuthApplicationService service;
    private Rs256JwtSupport jwt;
    private byte[] otpSecret;

    @BeforeEach
    void setUp() {
        users = new InMemoryUsers();
        sessions = new InMemorySessions();
        proofs = new InMemoryProofs();
        revoked = new InMemoryRevoked();
        jwt = Rs256JwtSupport.generateEphemeral("unit");
        otpSecret = new byte[] {9, 8, 7, 6, 5, 4, 3, 2, 1, 0};
        TokenIssuancePort tokens =
                new TokenIssuancePort() {
                    @Override
                    public IssuedAccessToken issueAccessToken(UUID userId) {
                        var token = jwt.issue(userId, "pg-platform-auth", "pg-platform",
                                new Rs256JwtSupport.DurationSpec(Duration.ofMinutes(15)));
                        var claims = jwt.parseUnverified(token);
                        return new IssuedAccessToken(token, claims.jti(), claims.expiresAt());
                    }

                    @Override
                    public VerifiedAccessToken verifyAccessToken(String token) {
                        var verified = jwt.verify(token, "pg-platform-auth", "pg-platform");
                        return new VerifiedAccessToken(verified.userId(), verified.jti(),
                                verified.expiresAt());
                    }

                    @Override
                    public UnverifiedAccessToken parseUnverified(String token) {
                        var claims = jwt.parseUnverified(token);
                        return new UnverifiedAccessToken(claims.jti(), claims.expiresAt());
                    }
                };
        PasswordHashPort passwords = new PasswordHashPort() {
            private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

            @Override
            public String hash(String rawPassword) {
                return hasher.hash(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String storedHash) {
                return hasher.matches(rawPassword, storedHash);
            }
        };
        service = new AuthApplicationService(
                passwords, tokens, revoked, users, sessions, proofs,
                otpSecret, Duration.ofDays(14), Duration.ofMinutes(5));
    }

    @Test
    void registerThenLoginReturnsTokens() {
        var registered = service.registerUser(
                new RegisterUserCommand(UUID.randomUUID(), "user@example.invalid",
                        "stronger1pass", "모의 사용자", "010-0000-0000"));
        assertNotNull(registered.userId());
        var duplicate = assertThrows(ApplicationException.class,
                () -> service.registerUser(new RegisterUserCommand(UUID.randomUUID(),
                        "user@example.invalid", "stronger1pass", "다른 사용자", null)));
        assertEquals(AuthErrorCode.EMAIL_ALREADY_EXISTS, duplicate.errorCode());

        var loggedIn = service.authenticate(new AuthenticateCommand(
                UUID.randomUUID(), "user@example.invalid", "stronger1pass"));
        assertNotNull(loggedIn.accessToken());
        assertNotNull(loggedIn.refreshToken());
        var me = service.getCurrentUser(
                new GetCurrentUserCommand(UUID.randomUUID(), registered.userId()));
        assertEquals("모의 사용자", me.name());
    }

    @Test
    void loginWithWrongPasswordFails() {
        service.registerUser(new RegisterUserCommand(UUID.randomUUID(), "user2@example.invalid",
                "stronger1pass", "모의 사용자", null));
        var error = assertThrows(ApplicationException.class,
                () -> service.authenticate(new AuthenticateCommand(UUID.randomUUID(),
                        "user2@example.invalid", "wrong-pass1")));
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, error.errorCode());
    }

    @Test
    void refreshRotatesSessionAndRejectsReuse() {
        var registered = service.registerUser(new RegisterUserCommand(UUID.randomUUID(),
                "user3@example.invalid", "stronger1pass", "모의 사용자", null));
        var login = service.authenticate(new AuthenticateCommand(UUID.randomUUID(),
                "user3@example.invalid", "stronger1pass"));
        var rotated = service.refreshToken(
                new RefreshTokenCommand(UUID.randomUUID(), login.refreshToken()));
        assertNotNull(rotated.accessToken());
        assertFalse(rotated.refreshToken().equals(login.refreshToken()));

        var reuse = assertThrows(ApplicationException.class,
                () -> service.refreshToken(
                        new RefreshTokenCommand(UUID.randomUUID(), login.refreshToken())));
        assertEquals(AuthErrorCode.TOKEN_REUSED, reuse.errorCode());

        var rotatedAgain = assertThrows(ApplicationException.class,
                () -> service.refreshToken(
                        new RefreshTokenCommand(UUID.randomUUID(), rotated.refreshToken())));
        assertEquals(AuthErrorCode.TOKEN_REUSED, rotatedAgain.errorCode());
        assertEquals(registered.userId(),
                users.findById(registered.userId()).orElseThrow().id());
    }

    @Test
    void logoutRevokesAccessJti() {
        service.registerUser(new RegisterUserCommand(UUID.randomUUID(), "user4@example.invalid",
                "stronger1pass", "모의 사용자", null));
        var login = service.authenticate(new AuthenticateCommand(UUID.randomUUID(),
                "user4@example.invalid", "stronger1pass"));
        service.logout(new LogoutCommand(UUID.randomUUID(), null,
                "Bearer " + login.accessToken()));
        assertEquals(1, revoked.revoked.size());
    }

    @Test
    void verifyOtpIssuesOneTimeTokenAndRejectsReplay() {
        var registered = service.registerUser(new RegisterUserCommand(UUID.randomUUID(),
                "user5@example.invalid", "stronger1pass", "모의 사용자", null));
        var draftId = UUID.randomUUID();
        var wrong = assertThrows(ApplicationException.class,
                () -> service.verifyOtp(new VerifyOtpCommand(UUID.randomUUID(),
                        registered.userId(), draftId, "000000")));
        assertEquals(AuthErrorCode.OTP_INVALID, wrong.errorCode());

        var seed = TotpMock.seedFor(otpSecret, registered.userId());
        var code = TotpMock.codeAt(seed, Instant.now());
        var verified = service.verifyOtp(
                new VerifyOtpCommand(UUID.randomUUID(), registered.userId(), draftId, code));
        assertNotNull(verified.otpToken());

        var replay = assertThrows(ApplicationException.class,
                () -> service.verifyOtp(new VerifyOtpCommand(UUID.randomUUID(),
                        registered.userId(), draftId, code)));
        assertEquals(AuthErrorCode.OTP_INVALID, replay.errorCode());
    }

    @Test
    void verifyOtpLocksAfterFiveFailures() {
        var registered = service.registerUser(new RegisterUserCommand(UUID.randomUUID(),
                "user6@example.invalid", "stronger1pass", "모의 사용자", null));
        var draftId = UUID.randomUUID();
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(ApplicationException.class,
                    () -> service.verifyOtp(new VerifyOtpCommand(UUID.randomUUID(),
                            registered.userId(), draftId, "000000")));
        }
        var seed = TotpMock.seedFor(otpSecret, registered.userId());
        var code = TotpMock.codeAt(seed, Instant.now());
        var locked = assertThrows(ApplicationException.class,
                () -> service.verifyOtp(new VerifyOtpCommand(UUID.randomUUID(),
                        registered.userId(), draftId, code)));
        assertEquals(AuthErrorCode.OTP_LOCKED, locked.errorCode());
    }

    static final class InMemoryUsers implements UserRepository {
        private final Map<UUID, User> byId = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void save(User value) {
            byId.put(value.id(), value);
            byEmail.put(value.email(), value);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email));
        }
    }

    static final class InMemorySessions implements RefreshSessionRepository {
        private final Map<UUID, RefreshSession> byId = new HashMap<>();
        private final Map<String, RefreshSession> byHash = new HashMap<>();
        private final List<UUID> revokedFamilies = new ArrayList<>();

        @Override
        public Optional<RefreshSession> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void save(RefreshSession value) {
            byId.put(value.id(), value);
            byHash.put(value.tokenHash(), value);
        }

        @Override
        public Optional<RefreshSession> findByTokenHash(String tokenHash) {
            return Optional.ofNullable(byHash.get(tokenHash));
        }

        @Override
        public void revokeFamily(UUID familyId, Instant revokedAt) {
            revokedFamilies.add(familyId);
            byId.replaceAll((id, session) -> session.familyId().equals(familyId)
                    ? new RefreshSession(session.id(), session.userId(), session.tokenHash(),
                            session.familyId(), session.expiresAt(), revokedAt)
                    : session);
            byHash.replaceAll((hash, session) -> session.familyId().equals(familyId)
                    ? new RefreshSession(session.id(), session.userId(), session.tokenHash(),
                            session.familyId(), session.expiresAt(), revokedAt)
                    : session);
        }
    }

    static final class InMemoryProofs implements OtpProofRepository {
        private final Map<UUID, OtpProof> byId = new LinkedHashMap<>();

        @Override
        public Optional<OtpProof> findById(UUID id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public void save(OtpProof value) {
            byId.put(value.id(), value);
        }

        /** 최신 증적을 반환한다(실제 어댑터는 created_at 내림차순과 동일하게 삽입 순서의 마지막). */
        @Override
        public Optional<OtpProof> findLatestByUserIdAndTransferDraftId(UUID userId, UUID draftId) {
            return byId.values().stream()
                    .filter(p -> p.userId().equals(userId) && p.transferDraftId().equals(draftId))
                    .reduce((first, second) -> second);
        }
    }

    static final class InMemoryRevoked implements RevokedTokenStorePort {
        private final Map<String, Instant> revoked = new HashMap<>();

        @Override
        public void revokeUntilExpiry(String jti, Instant expiresAt) {
            revoked.put(jti, expiresAt);
        }
    }
}

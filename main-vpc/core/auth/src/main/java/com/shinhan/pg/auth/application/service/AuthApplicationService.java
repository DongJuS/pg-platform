package com.shinhan.pg.auth.application.service;

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
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class AuthApplicationService
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

    public AuthApplicationService(
            PasswordHashPort passwordHashPort,
            TokenIssuancePort tokenIssuancePort,
            RevokedTokenStorePort revokedTokenStorePort,
            UserRepository userRepository,
            RefreshSessionRepository refreshSessionRepository,
            OtpProofRepository otpProofRepository) {
        this.passwordHashPort = java.util.Objects.requireNonNull(passwordHashPort);
        this.tokenIssuancePort = java.util.Objects.requireNonNull(tokenIssuancePort);
        this.revokedTokenStorePort = java.util.Objects.requireNonNull(revokedTokenStorePort);
        this.userRepository = java.util.Objects.requireNonNull(userRepository);
        this.refreshSessionRepository = java.util.Objects.requireNonNull(refreshSessionRepository);
        this.otpProofRepository = java.util.Objects.requireNonNull(otpProofRepository);
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. JWT RS256 서명, 실패 횟수 집계(Redis rate limit)
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981c69805e570fa72cf1b
     * </pre>
     */
    @Override
    public AuthenticateResult authenticate(AuthenticateCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: JWT RS256 서명, 실패 횟수 집계(Redis rate limit)
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F02");
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 이메일 중복 차단, 비밀번호 BCrypt 해시 저장
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ee85fdfeca462ac388
     * </pre>
     */
    @Override
    public RegisterUserResult registerUser(RegisterUserCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 이메일 중복 차단, 비밀번호 BCrypt 해시 저장
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F01");
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. refresh 회전, 재사용 감지 시 전체 철회
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498100bb77d2811deb087f
     * </pre>
     */
    @Override
    public RefreshTokenResult refreshToken(RefreshTokenCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: refresh 회전, 재사용 감지 시 전체 철회
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F03");
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 토큰 jti를 Redis 철회 목록에 만료 시각까지 저장
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 기본 로그아웃은 access jti 철회다. refresh 세션 전체 철회·전체 기기 로그아웃(MVP1-F18)은 보류하며 이 메서드에 자동 추가하지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49816c96c5d41e6ffbcb52
     * </pre>
     */
    @Override
    public void logout(LogoutCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 토큰 jti를 Redis 철회 목록에 만료 시각까지 저장
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F04");
    }

    /**
     * <pre>
     * [MVP1-F05] 내 정보 조회
     * API 초안: GET /api/v1/users/me
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: —
     * 출력 근거: userId, 이름, 권한
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 401
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 토큰의 userId로만 조회
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981019060eb224973005c
     * </pre>
     */
    @Override
    public GetCurrentUserResult getCurrentUser(GetCurrentUserCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 토큰의 userId로만 조회
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F05");
    }

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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. Mock OTP 검증, 실패 횟수 누적
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 실제 OTP 제공사 연동이 아니라 Mock OTP다. proof의 유효시간·소모 조건은 후속 계약이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @Override
    public VerifyOtpResult verifyOtp(VerifyOtpCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: Mock OTP 검증, 실패 횟수 누적
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F06");
    }
}

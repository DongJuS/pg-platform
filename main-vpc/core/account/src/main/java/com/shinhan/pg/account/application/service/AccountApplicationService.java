package com.shinhan.pg.account.application.service;

import com.shinhan.pg.account.application.command.ChangeAccountStatusCommand;
import com.shinhan.pg.account.application.command.GetAccountCommand;
import com.shinhan.pg.account.application.command.ListAccountsCommand;
import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.port.in.ChangeAccountStatusUseCase;
import com.shinhan.pg.account.application.port.in.GetAccountUseCase;
import com.shinhan.pg.account.application.port.in.ListAccountsUseCase;
import com.shinhan.pg.account.application.port.in.OpenAccountUseCase;
import com.shinhan.pg.account.application.port.out.AccountRepository;
import com.shinhan.pg.account.application.port.out.AccountWritePort;
import com.shinhan.pg.account.application.port.out.LedgerAccountPort;
import com.shinhan.pg.account.application.readmodel.AccountSummary;
import com.shinhan.pg.account.application.result.ChangeAccountStatusResult;
import com.shinhan.pg.account.application.result.GetAccountResult;
import com.shinhan.pg.account.application.result.ListAccountsResult;
import com.shinhan.pg.account.application.result.OpenAccountResult;
import com.shinhan.pg.account.domain.error.AccountErrorCode;
import com.shinhan.pg.account.domain.model.Account;
import com.shinhan.pg.account.domain.policy.AccountStatusPolicy;
import com.shinhan.pg.common.commonerror.ApplicationException;

import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * MVP1 업무는 실제 저장소를 사용하며, 범위 밖의 미구현 기능만 501을 반환한다.
 *
 * <p>구현 확정(MVP1-F07~F10): MariaDB accounts 테이블 기반으로 실동작한다. 잔액은 다루지 않는다(ledger 소유).
 * 인증 주체는 gateway가 JWT 검증 후 전달한 X-User-Id → actorUserId다. PUBLIC 메서드는 actorUserId가
 * 없으면 401(UNAUTHENTICATED)로 거절한다(인증 문맥 계약 §3).
 */
public class AccountApplicationService
        implements com.shinhan.pg.account.application.port.in.InspectAccountUseCase, OpenAccountUseCase,
                ListAccountsUseCase,
                GetAccountUseCase,
                ChangeAccountStatusUseCase {
    /** [MVP1-F07] 입력 근거 "통화(KRW)": MVP1은 KRW만 받는다. TODO[CONTRACT]: 지원 통화 확장은 미정 정책 — 임의 확정 금지. */
    private static final String MVP1_CURRENCY = "KRW";

    private static final int DEFAULT_PAGE = 0;
    // TODO[CONTRACT]: 페이지 기본/최대 크기는 최종 계약 전까지의 보수적 임시값이다.
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final LedgerAccountPort ledgerAccountPort;
    private final AccountWritePort accountWritePort;
    private final AccountRepository accountRepository;
    private final AccountStatusPolicy accountStatusPolicy = new AccountStatusPolicy();

    public AccountApplicationService(
            LedgerAccountPort ledgerAccountPort,
            AccountWritePort accountWritePort,
            AccountRepository accountRepository) {
        this.ledgerAccountPort = java.util.Objects.requireNonNull(ledgerAccountPort);
        this.accountWritePort = java.util.Objects.requireNonNull(accountWritePort);
        this.accountRepository = java.util.Objects.requireNonNull(accountRepository);
    }

    /**
     * <pre>
     * [MVP1-F07] 가상 계좌 개설
     * API 초안: POST /api/v1/accounts
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 통화(KRW), 계좌 별칭
     * 출력 근거: accountId, 계좌번호
     * 정상 응답 후보: 201
     * 오류 후보: 400
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 실계좌 아님, 상태 ACTIVE로 생성
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49812bb64ff9c7f4ac5c09
     * </pre>
     */
    @Override
    @Transactional
    public OpenAccountResult openAccount(OpenAccountCommand command) {
        // 1. 인증된 주체·자원 접근 범위와 입력을 확인한다.
        requireActor(command.actorUserId());
        if (command.currency() == null || !MVP1_CURRENCY.equals(command.currency())) {
            // TODO[CONTRACT]: 지원 통화 목록은 미정 — MVP1은 입력 근거대로 KRW만 허용한다.
            throw new ApplicationException(AccountErrorCode.INVALID_REQUEST);
        }
        if (command.alias() != null && command.alias().length() > 100) {
            throw new ApplicationException(AccountErrorCode.INVALID_REQUEST);
        }
        // 2. 실계좌 아님, 상태 ACTIVE로 생성 — AccountWriteAdapter가 INSERT(계좌번호 서버 생성, unique 충돌 시 1회 재시도).
        // 3. 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO[LEDGER-PROVISION]: 계좌 개설 시 선행 원장 계정 생성은 하지 않는다.
        //   account DB만 생성하며, Ledger 조회는 Account 확인 후 0원, 최초 분개는 lazy upsert로 처리한다.
        //   기존 provision 포트는 사용하지 않는다. 외부 HTTP를 금융 DB 트랜잭션 안에서 호출하지 않는다.
        java.util.Objects.requireNonNull(this.ledgerAccountPort); // provision 계약 확정 전까지 참조만 유지
        return accountWritePort.create(command);
        // 4. 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다 — 결과는 OpenAccountResult로 반환.
    }

    /**
     * <pre>
     * [MVP1-F08] 내 계좌 목록
     * API 초안: GET /api/v1/accounts
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: page, size
     * 출력 근거: 계좌 목록
     * 정상 응답 후보: 200
     * 오류 후보: 401
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 본인 계좌만 조회
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981b59cfff3bb73f5629c
     * </pre>
     */
    @Override
    @Transactional(readOnly = true)
    public ListAccountsResult listAccounts(ListAccountsCommand command) {
        // 1. 인증된 주체 확인.
        requireActor(command.actorUserId());
        int page = command.page() == null ? DEFAULT_PAGE : command.page();
        int size = command.size() == null ? DEFAULT_SIZE : command.size();
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(AccountErrorCode.INVALID_REQUEST);
        }
        // 2. 본인 계좌만 조회 — owner_id = 인증 주체. 다음 페이지 존재 여부는 size+1 조회로 판단.
        var rows =
                accountRepository.findByOwnerId(command.actorUserId(), page * size, size + 1);
        var hasMore = rows.size() > size;
        var accounts =
                rows.stream()
                        .limit(size)
                        .map(
                                account ->
                                        new AccountSummary(
                                                account.id(),
                                                account.accountNumber(),
                                                account.alias(),
                                                account.currency(),
                                                account.status()))
                        .toList();
        // 3~4. 조회는 상태를 바꾸지 않는다. 결과 DTO로 반환.
        return new ListAccountsResult(accounts, hasMore ? page + 1 : null);
    }

    /**
     * <pre>
     * [MVP1-F09] 계좌 상세
     * API 초안: GET /api/v1/accounts/{accountId}
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: accountId
     * 출력 근거: 계좌 메타데이터
     * 정상 응답 후보: 200
     * 오류 후보: 403 타인 계좌, 404
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 소유권 검사, 잔액은 Ledger 담당
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498153b3bff10d8ad2b396
     * </pre>
     */
    @Override
    @Transactional(readOnly = true)
    public GetAccountResult getAccount(GetAccountCommand command) {
        // 1. 인증된 주체 확인.
        requireActor(command.actorUserId());
        // 2. 소유권 검사: 없으면 404, 타인 소유면 403. 잔액은 Ledger 담당 — 여기서 조회하지 않는다.
        var account =
                accountRepository
                        .findById(command.accountId())
                        .orElseThrow(
                                () -> new ApplicationException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.ownerId().equals(command.actorUserId())) {
            throw new ApplicationException(AccountErrorCode.ACCOUNT_FORBIDDEN);
        }
        // 3~4. 조회는 상태를 바꾸지 않는다. 결과 DTO로 반환.
        return new GetAccountResult(
                account.id(),
                account.ownerId(),
                account.accountNumber(),
                account.alias(),
                account.currency(),
                account.status());
    }

    /** Minimal account authority lookup for Ledger/Transfer; never returns a balance. */
    @Override @Transactional(readOnly = true)
    public GetAccountResult inspectAccount(UUID accountId) {
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ApplicationException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        return new GetAccountResult(account.id(), account.ownerId(), account.accountNumber(), account.alias(), account.currency(), account.status());
    }

    /**
     * <pre>
     * [MVP1-F10] 계좌 상태 변경
     * API 초안: PATCH /api/v1/accounts/{accountId}/status
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: status, 사유
     * 출력 근거: 변경된 상태
     * 정상 응답 후보: 200
     * 오류 후보: 409 허용되지 않는 전이
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 허용 전이만 처리 (ACTIVE↔SUSPENDED)
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 경로에 /api/v1이 있어도 내부 서비스 인증 대상이다. Gateway 외부 라우팅에서 제외할 조건을 적어둔다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49813aa4d2f0ad91e9efac
     * </pre>
     *
     * <p>구현 확정: INTERNAL 대상 — X-Internal-Caller 헤더 검증은 Controller가 수행한다(없으면 403).
     * 서비스 간 인증 토큰은 미정(지시서 §5-③)이므로 여기서는 소유권 검사를 하지 않고 전이 규칙만 적용한다.
     */
    @Override
    @Transactional
    public ChangeAccountStatusResult changeAccountStatus(ChangeAccountStatusCommand command) {
        // 1. 신뢰한 서비스 신원은 Controller의 X-Internal-Caller 검증으로 확인된다. 대상 계좌 존재 확인.
        var account =
                accountRepository
                        .findById(command.accountId())
                        .orElseThrow(
                                () -> new ApplicationException(AccountErrorCode.ACCOUNT_NOT_FOUND));
        // 2. 허용 전이만 처리 (ACTIVE↔SUSPENDED) — AccountStatusPolicy.
        accountStatusPolicy.validateTransition(account, command.status());
        // 3. 짧은 로컬 DB 트랜잭션(@Transactional). 외부 호출 없음.
        var updated =
                new Account(
                        account.id(),
                        account.ownerId(),
                        account.accountNumber(),
                        account.currency(),
                        account.alias(),
                        command.status());
        accountRepository.save(updated);
        // 4. 확정 결과를 DTO로 반환.
        return new ChangeAccountStatusResult(updated.id(), updated.status());
    }

    /** PUBLIC 메서드 공통: 인증 주체(X-User-Id → actorUserId)가 없으면 401로 거절한다(인증 문맥 계약 §3). */
    private static void requireActor(UUID actorUserId) {
        if (actorUserId == null) {
            throw new ApplicationException(AccountErrorCode.UNAUTHENTICATED);
        }
    }
}

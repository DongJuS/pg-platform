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
import com.shinhan.pg.account.application.result.ChangeAccountStatusResult;
import com.shinhan.pg.account.application.result.GetAccountResult;
import com.shinhan.pg.account.application.result.ListAccountsResult;
import com.shinhan.pg.account.application.result.OpenAccountResult;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class AccountApplicationService
        implements OpenAccountUseCase,
                ListAccountsUseCase,
                GetAccountUseCase,
                ChangeAccountStatusUseCase {
    private final LedgerAccountPort ledgerAccountPort;
    private final AccountWritePort accountWritePort;
    private final AccountRepository accountRepository;

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
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 통화(KRW), 계좌 별칭
     * 출력 근거: accountId, 계좌번호
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
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
    public OpenAccountResult openAccount(OpenAccountCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 실계좌 아님, 상태 ACTIVE로 생성
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F07");
    }

    /**
     * <pre>
     * [MVP1-F08] 내 계좌 목록
     * API 초안: GET /api/v1/accounts
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: page, size
     * 출력 근거: 계좌 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
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
    public ListAccountsResult listAccounts(ListAccountsCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 본인 계좌만 조회
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F08");
    }

    /**
     * <pre>
     * [MVP1-F09] 계좌 상세
     * API 초안: GET /api/v1/accounts/{accountId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: accountId
     * 출력 근거: 계좌 메타데이터
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
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
    public GetAccountResult getAccount(GetAccountCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 소유권 검사, 잔액은 Ledger 담당
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F09");
    }

    /**
     * <pre>
     * [MVP1-F10] 계좌 상태 변경
     * API 초안: PATCH /api/v1/accounts/{accountId}/status
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: status, 사유
     * 출력 근거: 변경된 상태
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
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
     */
    @Override
    public ChangeAccountStatusResult changeAccountStatus(ChangeAccountStatusCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 허용 전이만 처리 (ACTIVE↔SUSPENDED)
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP1-F10");
    }
}

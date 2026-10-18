package com.shinhan.pg.account.adapter.in.web;

import com.shinhan.pg.account.adapter.in.web.request.ChangeAccountStatusRequest;
import com.shinhan.pg.account.adapter.in.web.request.OpenAccountRequest;
import com.shinhan.pg.account.adapter.in.web.response.ChangeAccountStatusResponse;
import com.shinhan.pg.account.adapter.in.web.response.GetAccountResponse;
import com.shinhan.pg.account.adapter.in.web.response.ListAccountsResponse;
import com.shinhan.pg.account.adapter.in.web.response.OpenAccountResponse;
import com.shinhan.pg.account.application.command.ChangeAccountStatusCommand;
import com.shinhan.pg.account.application.command.GetAccountCommand;
import com.shinhan.pg.account.application.command.ListAccountsCommand;
import com.shinhan.pg.account.application.command.OpenAccountCommand;
import com.shinhan.pg.account.application.port.in.ChangeAccountStatusUseCase;
import com.shinhan.pg.account.application.port.in.GetAccountUseCase;
import com.shinhan.pg.account.application.port.in.ListAccountsUseCase;
import com.shinhan.pg.account.application.port.in.OpenAccountUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
@RestController
@Profile("skeleton")
public final class AccountController {
    private final OpenAccountUseCase openAccountUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final ChangeAccountStatusUseCase changeAccountStatusUseCase;

    public AccountController(
            OpenAccountUseCase openAccountUseCase,
            ListAccountsUseCase listAccountsUseCase,
            GetAccountUseCase getAccountUseCase,
            ChangeAccountStatusUseCase changeAccountStatusUseCase) {
        this.openAccountUseCase = java.util.Objects.requireNonNull(openAccountUseCase);
        this.listAccountsUseCase = java.util.Objects.requireNonNull(listAccountsUseCase);
        this.getAccountUseCase = java.util.Objects.requireNonNull(getAccountUseCase);
        this.changeAccountStatusUseCase =
                java.util.Objects.requireNonNull(changeAccountStatusUseCase);
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
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49812bb64ff9c7f4ac5c09
     * </pre>
     */
    @PostMapping("/api/v1/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public OpenAccountResponse openAccount(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody OpenAccountRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                openAccountUseCase.openAccount(
                        new OpenAccountCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.currency(),
                                request.alias()));
        return new OpenAccountResponse(
                result.accountId(), result.accountNumber(), result.currency(), result.status());
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
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981b59cfff3bb73f5629c
     * </pre>
     */
    @GetMapping("/api/v1/accounts")
    @ResponseStatus(HttpStatus.OK)
    public ListAccountsResponse listAccounts(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                listAccountsUseCase.listAccounts(
                        new ListAccountsCommand(UUID.randomUUID(), actorUserId, page, size));
        return new ListAccountsResponse(result.accounts(), result.nextPage());
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
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498153b3bff10d8ad2b396
     * </pre>
     */
    @GetMapping("/api/v1/accounts/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    public GetAccountResponse getAccount(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("accountId") UUID accountId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getAccountUseCase.getAccount(
                        new GetAccountCommand(UUID.randomUUID(), actorUserId, accountId));
        return new GetAccountResponse(
                result.accountId(),
                result.ownerId(),
                result.accountNumber(),
                result.alias(),
                result.currency(),
                result.status());
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
     * TODO: 경로에 /api/v1이 있어도 내부 서비스 인증 대상이다. Gateway 외부 라우팅에서 제외할 조건을 적어둔다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49813aa4d2f0ad91e9efac
     * </pre>
     */
    @PatchMapping("/api/v1/accounts/{accountId}/status")
    @ResponseStatus(HttpStatus.OK)
    public ChangeAccountStatusResponse changeAccountStatus(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody ChangeAccountStatusRequest request,
            @PathVariable("accountId") UUID accountId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                changeAccountStatusUseCase.changeAccountStatus(
                        new ChangeAccountStatusCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.status(),
                                request.reason(),
                                accountId));
        return new ChangeAccountStatusResponse(result.accountId(), result.status());
    }
}

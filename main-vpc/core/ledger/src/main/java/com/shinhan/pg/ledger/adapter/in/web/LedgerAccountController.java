package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.ledger.adapter.in.web.response.GetBalanceResponse;
import com.shinhan.pg.ledger.adapter.in.web.response.ListJournalEntriesResponse;
import com.shinhan.pg.ledger.application.command.GetBalanceCommand;
import com.shinhan.pg.ledger.application.command.ListJournalEntriesCommand;
import com.shinhan.pg.ledger.application.port.in.GetBalanceUseCase;
import com.shinhan.pg.ledger.application.port.in.ListJournalEntriesUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
@RestController
@Profile("skeleton")
public final class LedgerAccountController {
    private final GetBalanceUseCase getBalanceUseCase;
    private final ListJournalEntriesUseCase listJournalEntriesUseCase;

    public LedgerAccountController(
            GetBalanceUseCase getBalanceUseCase,
            ListJournalEntriesUseCase listJournalEntriesUseCase) {
        this.getBalanceUseCase = java.util.Objects.requireNonNull(getBalanceUseCase);
        this.listJournalEntriesUseCase =
                java.util.Objects.requireNonNull(listJournalEntriesUseCase);
    }

    /**
     * <pre>
     * [MVP1-F11] 잔액 조회
     * API 초안: GET /api/v1/accounts/{accountId}/balances
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: accountId
     * 출력 근거: 잔액, 기준 시각
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981b9b6c9fc7a3de22119
     * </pre>
     */
    @GetMapping("/api/v1/accounts/{accountId}/balances")
    @ResponseStatus(HttpStatus.OK)
    public GetBalanceResponse getBalance(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("accountId") UUID accountId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getBalanceUseCase.getBalance(
                        new GetBalanceCommand(UUID.randomUUID(), actorUserId, accountId));
        return new GetBalanceResponse(
                result.accountId(), result.balance(), result.currency(), result.asOf());
    }

    /**
     * <pre>
     * [MVP1-F12] 원장 거래내역
     * API 초안: GET /api/v1/accounts/{accountId}/entries
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: from, to, cursor
     * 출력 근거: entry 목록, nextCursor
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ddbaffdc6c0103bf25
     * </pre>
     */
    @GetMapping("/api/v1/accounts/{accountId}/entries")
    @ResponseStatus(HttpStatus.OK)
    public ListJournalEntriesResponse listJournalEntries(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("accountId") UUID accountId,
            @RequestParam(name = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(name = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", required = false) Integer size) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                listJournalEntriesUseCase.listJournalEntries(
                        new ListJournalEntriesCommand(
                                UUID.randomUUID(), actorUserId, accountId, from, to, cursor, size));
        return new ListJournalEntriesResponse(result.entries(), result.nextCursor());
    }
}

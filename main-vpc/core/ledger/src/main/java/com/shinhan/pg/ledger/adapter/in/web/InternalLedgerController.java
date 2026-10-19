package com.shinhan.pg.ledger.adapter.in.web;

import com.shinhan.pg.ledger.adapter.in.web.request.PostJournalRequest;
import com.shinhan.pg.ledger.adapter.in.web.response.PostJournalResponse;
import com.shinhan.pg.ledger.adapter.in.web.response.QueryJournalResponse;
import com.shinhan.pg.ledger.application.command.PostJournalCommand;
import com.shinhan.pg.ledger.application.command.QueryJournalCommand;
import com.shinhan.pg.ledger.application.port.in.PostJournalUseCase;
import com.shinhan.pg.ledger.application.port.in.QueryJournalUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
@RestController
@Profile("skeleton")
public final class InternalLedgerController {
    private final PostJournalUseCase postJournalUseCase;
    private final QueryJournalUseCase queryJournalUseCase;

    public InternalLedgerController(
            PostJournalUseCase postJournalUseCase, QueryJournalUseCase queryJournalUseCase) {
        this.postJournalUseCase = java.util.Objects.requireNonNull(postJournalUseCase);
        this.queryJournalUseCase = java.util.Objects.requireNonNull(queryJournalUseCase);
    }

    /**
     * <pre>
     * [MVP1-F13] 분개 반영
     * API 초안: POST /internal/v1/ledger/postings
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: transactionId, posting 목록
     * 출력 근거: journalId
     * 정상 응답 후보: 200
     * 오류 후보: 409 중복(기존 journal 반환), 422 합계≠0
     * TODO: ledger.posted만 원장 Outbox에 저장한다. transfer.completed는 Transfer의 책임이다.
     * TODO: 계좌 ID 정렬 잠금 → 잔액/통화별 합계 0 검증 → 분개·잔액·Outbox 같은 MariaDB 트랜잭션. 외부 HTTP를 잠금 중 호출하지 않는다.
     * TODO: 원장 동일 transactionId 결과 재조회와 클라이언트 중복 이체 409는 서로 다른 계약이다. 409/기존 journal 응답의 최종 형식은 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498164b8eeca1655e74d32
     * </pre>
     */
    @PostMapping("/internal/v1/ledger/postings")
    @ResponseStatus(HttpStatus.OK)
    public PostJournalResponse postJournal(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @RequestBody PostJournalRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                postJournalUseCase.postJournal(
                        new PostJournalCommand(
                                UUID.randomUUID(),
                                callerServiceId,
                                request.transactionId(),
                                request.postings()));
        return new PostJournalResponse(
                result.transactionId(), result.journalId(), result.eventId());
    }

    /**
     * <pre>
     * [MVP1-F14] 반영 결과 조회
     * API 초안: GET /internal/v1/ledger/transactions/{transactionId}
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: transactionId
     * 출력 근거: journal 또는 404
     * 정상 응답 후보: 200
     * 오류 후보: 404 미반영
     * TODO: 조회 404만으로 이체의 최종 실패를 단정하지 않는다. 처리 중/불명과 미반영의 구분은 후속 계약이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981f79aa8dadf63136167
     * </pre>
     */
    @GetMapping("/internal/v1/ledger/transactions/{transactionId}")
    @ResponseStatus(HttpStatus.OK)
    public QueryJournalResponse queryJournal(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @PathVariable("transactionId") UUID transactionId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                queryJournalUseCase.queryJournal(
                        new QueryJournalCommand(UUID.randomUUID(), callerServiceId, transactionId));
        return new QueryJournalResponse(
                result.transactionId(), result.journalId(), result.eventId(), result.status());
    }
}

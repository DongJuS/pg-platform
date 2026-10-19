package com.shinhan.pg.transfer.adapter.out.ledger;

import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.out.LedgerOutcomeUnknownException;
import com.shinhan.pg.transfer.application.port.out.LedgerPostingPort;
import com.shinhan.pg.transfer.application.readmodel.LedgerPostingReceiptDraft;
import com.shinhan.pg.transfer.domain.error.TransferErrorCode;
import com.shinhan.pg.transfer.domain.error.TransferException;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 동일 transactionId로 내부 원장 REST를 호출·조회한다. 타임아웃을 새 출금으로 대체하지 않는다.
 *
 * <p>WO-A6 실구현(LedgerPostingRestSkeleton 대체). 호출자 소유 클라이언트로
 * {@code adapter.out.ledger} 패키지에 둔다(ArchUnit R7). wire 계약은 ledger의
 * POST /internal/v1/ledger/postings(PostJournalRequest/PostJournalResponse)와
 * GET /internal/v1/ledger/transactions/{transactionId}(QueryJournalResponse)를 그대로 사용한다 —
 * ledger 측 타입을 import하지 않고 이 패키지의 wire record로 대조한다(R5).</p>
 *
 * <p>실패 분류(ADR 0001): 타임아웃·연결 실패·5xx·그 외 비정상은 결과 불명이다. 즉시 실패로
 * 확정하지 않고 {@link LedgerOutcomeUnknownException}을 던져 호출자가 UNKNOWN 전이 + 동일
 * transactionId 재조회로 수렴하게 한다. 원장의 명시적 업무 거절(422 잔액 부족·합계≠0)만
 * 최종 실패 신호({@link TransferException} INSUFFICIENT_FUNDS)로 변환한다. 이 클래스에서
 * 재시도·재출금은 없다.</p>
 */
public final class LedgerPostingRestAdapter implements LedgerPostingPort {

    /** 서비스 간 호출 표식 헤더(§3 인증 문맥 계약). 서비스 인증 토큰은 미정(§5-③). */
    static final String INTERNAL_CALLER_HEADER = "X-Internal-Caller";

    static final String INTERNAL_CALLER_VALUE = "transfer";

    private final RestClient rest;
    private final String internalServiceToken;

    public LedgerPostingRestAdapter(LedgerClientProperties properties) {
        Objects.requireNonNull(properties);
        this.internalServiceToken = properties.internalServiceToken();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        factory.setReadTimeout((int) properties.readTimeout().toMillis());
        this.rest =
                RestClient.builder()
                        .baseUrl(properties.baseUrl())
                        .requestFactory(factory)
                        .build();
    }

    /** 테스트용 — 대상/직렬화를 제어한 RestClient를 직접 주입한다. */
    LedgerPostingRestAdapter(RestClient rest) {
        this.rest = Objects.requireNonNull(rest);
        this.internalServiceToken = "";
    }

    /** ledger PostJournalRequest 계약 대응 wire DTO. */
    record PostJournalWireRequest(UUID transactionId, List<PostingWire> postings) {}

    /** ledger PostingInstruction 계약 대응 wire DTO. */
    record PostingWire(UUID accountId, BigDecimal signedAmount, String currency) {}

    /** ledger PostJournalResponse 계약 대응 wire DTO. */
    record PostJournalWireResponse(UUID transactionId, UUID journalId, UUID eventId) {}

    /** ledger QueryJournalResponse 계약 대응 wire DTO. */
    record QueryJournalWireResponse(UUID transactionId, UUID journalId, UUID eventId, String status) {}

    @Override
    public LedgerPostingReceiptDraft post(UUID transactionId, RequestTransferCommand command) {
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(command);
        var request =
                new PostJournalWireRequest(
                        transactionId,
                        List.of(
                                new PostingWire(
                                        command.fromAccountId(),
                                        command.amount().negate(),
                                        command.currency()),
                                new PostingWire(
                                        command.toAccountId(),
                                        command.amount(),
                                        command.currency())));
        try {
            var response =
                    rest.post()
                            .uri("/internal/v1/ledger/postings")
                            .header(INTERNAL_CALLER_HEADER, INTERNAL_CALLER_VALUE)
                            .header("X-Internal-Service-Token", internalServiceToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(PostJournalWireResponse.class);
            if (response == null || response.transactionId() == null || response.journalId() == null) {
                throw unknown("ledger posting returned an incomplete response", null);
            }
            return new LedgerPostingReceiptDraft(
                    response.transactionId(), response.journalId(), response.eventId());
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                // 원장이 명시적으로 거절(잔액 부족·합계≠0) — 분개가 반영되지 않았음이 확정된 유일한 경우.
                throw new TransferException(
                        TransferErrorCode.INSUFFICIENT_FUNDS,
                        "Ledger rejected the posting (422)");
            }
            throw unknown("ledger posting failed with HTTP " + error.getStatusCode(), error);
        } catch (ResourceAccessException error) {
            // connect/read 타임아웃·연결 거부 — 결과 불명. 새 출금으로 대체하지 않는다.
            throw unknown("ledger posting outcome unknown (transport)", error);
        } catch (RestClientException error) {
            throw unknown("ledger posting outcome unknown", error);
        }
    }

    @Override
    public Optional<LedgerPostingReceiptDraft> findJournal(UUID transactionId) {
        Objects.requireNonNull(transactionId);
        try {
            var response =
                    rest.get()
                            .uri("/internal/v1/ledger/transactions/{transactionId}", transactionId)
                            .header(INTERNAL_CALLER_HEADER, INTERNAL_CALLER_VALUE)
                            .header("X-Internal-Service-Token", internalServiceToken)
                            .retrieve()
                            .body(QueryJournalWireResponse.class);
            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(
                    new LedgerPostingReceiptDraft(
                            response.transactionId(), response.journalId(), response.eventId()));
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                // 404는 미반영 신호일 뿐 최종 실패가 아니다(포트 계약).
                return Optional.empty();
            }
            throw unknown("ledger journal query failed with HTTP " + error.getStatusCode(), error);
        } catch (ResourceAccessException error) {
            throw unknown("ledger journal query outcome unknown (transport)", error);
        } catch (RestClientException error) {
            throw unknown("ledger journal query outcome unknown", error);
        }
    }

    private LedgerOutcomeUnknownException unknown(String message, Throwable cause) {
        LoggerFactory.getLogger(getClass()).warn("{} — transfer stays UNKNOWN", message, cause);
        return new LedgerOutcomeUnknownException(message, cause);
    }
}

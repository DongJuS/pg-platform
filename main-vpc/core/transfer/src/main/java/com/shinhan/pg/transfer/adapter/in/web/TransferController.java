package com.shinhan.pg.transfer.adapter.in.web;

import com.shinhan.pg.transfer.adapter.in.web.request.RequestTransferRequest;
import com.shinhan.pg.transfer.adapter.in.web.response.GetTransferResponse;
import com.shinhan.pg.transfer.adapter.in.web.response.ListTransfersResponse;
import com.shinhan.pg.transfer.adapter.in.web.response.RequestTransferResponse;
import com.shinhan.pg.transfer.application.command.GetTransferCommand;
import com.shinhan.pg.transfer.application.command.ListTransfersCommand;
import com.shinhan.pg.transfer.application.command.RequestTransferCommand;
import com.shinhan.pg.transfer.application.port.in.GetTransferUseCase;
import com.shinhan.pg.transfer.application.port.in.ListTransfersUseCase;
import com.shinhan.pg.transfer.application.port.in.RequestTransferUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 * WO-A6: 인증 문맥 연결 — gateway가 JWT 검증 후 전달하는 X-User-Id 헤더를 신뢰 주체로 사용한다(§3 계약).
 * 헤더 부재 시 null을 넘겨 유스케이스가 401로 거절하게 하고, 형식 오류는 400으로 거절한다.
 */
@RestController
@Profile("skeleton")
public final class TransferController {
    private final RequestTransferUseCase requestTransferUseCase;
    private final GetTransferUseCase getTransferUseCase;
    private final ListTransfersUseCase listTransfersUseCase;

    public TransferController(
            RequestTransferUseCase requestTransferUseCase,
            GetTransferUseCase getTransferUseCase,
            ListTransfersUseCase listTransfersUseCase) {
        this.requestTransferUseCase = java.util.Objects.requireNonNull(requestTransferUseCase);
        this.getTransferUseCase = java.util.Objects.requireNonNull(getTransferUseCase);
        this.listTransfersUseCase = java.util.Objects.requireNonNull(listTransfersUseCase);
    }

    /**
     * <pre>
     * [MVP1-F15] 가상 이체
     * API 초안: POST /api/v1/transfers
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: Idempotency-Key 헤더, 출금·입금 계좌, 금액, otpToken
     * 출력 근거: transferId, 상태; 중복 요청은 오류
     * 정상 응답 후보: 200
     * 오류 후보: 409 중복 요청·키 충돌, 422 잔액 부족, 401 OTP
     * TODO: actorUserId + idempotencyKey 고유 조건과 요청 해시를 예약한다. 같은 키 재요청은 사용자 결정대로 409 오류이며 신규 이체를 만들지 않는다.
     * TODO: 원장 REST 성공과 ledger.posted 소비는 동일 완료 경로로 수렴시킨다. 시간 초과는 UNKNOWN으로 두고 동일 transactionId로 조회한다.
     * TODO: 성공 200은 동기 완료의 응답 초안이다. 접수/결과불명 시 HTTP 응답 계약은 미정이며 코드로 성공을 꾸미지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981ae8beff34a15fd47f1
     * </pre>
     */
    @PostMapping("/api/v1/transfers")
    @ResponseStatus(HttpStatus.OK)
    public RequestTransferResponse requestTransfer(
            @RequestHeader(name = "X-User-Id", required = false) String actorUserIdHeader,
            @RequestBody RequestTransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        // otpToken은 Auth가 서명·사용자·만료·요청 바인딩을 검증하고 소비한다.
        var result =
                requestTransferUseCase.requestTransfer(
                        new RequestTransferCommand(
                                UUID.randomUUID(),
                                parseActorUserId(actorUserIdHeader),
                                request.fromAccountId(),
                                request.toAccountId(),
                                request.amount(),
                                request.currency(),
                                request.otpToken(),
                                idempotencyKey));
        return new RequestTransferResponse(
                result.transferId(), result.transactionId(), result.status());
    }

    /**
     * <pre>
     * [MVP1-F16] 이체 상태 조회
     * API 초안: GET /api/v1/transfers/{transferId}
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: transferId
     * 출력 근거: 상태, 전이 이력
     * 정상 응답 후보: 200
     * 오류 후보: 403, 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498126a203df4f8dd678f7
     * </pre>
     */
    @GetMapping("/api/v1/transfers/{transferId}")
    @ResponseStatus(HttpStatus.OK)
    public GetTransferResponse getTransfer(
            @RequestHeader(name = "X-User-Id", required = false) String actorUserIdHeader,
            @PathVariable("transferId") UUID transferId) {
        var result =
                getTransferUseCase.getTransfer(
                        new GetTransferCommand(
                                UUID.randomUUID(), parseActorUserId(actorUserIdHeader), transferId));
        return new GetTransferResponse(
                result.transferId(), result.transactionId(), result.status(), result.history());
    }

    /**
     * <pre>
     * [MVP1-F17] 이체 목록
     * API 초안: GET /api/v1/transfers
     * 상태: 구현 — 인증·권한·업무 검증 후 처리한다
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 기간, 상태, page
     * 출력 근거: 이체 목록
     * 정상 응답 후보: 200
     * 오류 후보: 401
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a498101b674c0841cd7ad05
     * </pre>
     */
    @GetMapping("/api/v1/transfers")
    @ResponseStatus(HttpStatus.OK)
    public ListTransfersResponse listTransfers(
            @RequestHeader(name = "X-User-Id", required = false) String actorUserIdHeader,
            @RequestParam(name = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(name = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        var result =
                listTransfersUseCase.listTransfers(
                        new ListTransfersCommand(
                                UUID.randomUUID(),
                                parseActorUserId(actorUserIdHeader),
                                from,
                                to,
                                status,
                                page,
                                size));
        return new ListTransfersResponse(result.transfers(), result.nextPage());
    }

    /** X-User-Id 헤더를 신뢰 주체로 변환한다. 부재 시 null(유스케이스가 401), 형식 오류는 400. */
    private static UUID parseActorUserId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException error) {
            throw new com.shinhan.pg.transfer.domain.error.TransferException(
                    com.shinhan.pg.transfer.domain.error.TransferErrorCode.INVALID_REQUEST,
                    "Malformed X-User-Id header");
        }
    }
}

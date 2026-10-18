package com.shinhan.pg.settlement.adapter.in.web;

import com.shinhan.pg.settlement.adapter.in.web.request.RerunSettlementRequest;
import com.shinhan.pg.settlement.adapter.in.web.response.GetSettlementResponse;
import com.shinhan.pg.settlement.adapter.in.web.response.RerunSettlementResponse;
import com.shinhan.pg.settlement.application.command.GetSettlementCommand;
import com.shinhan.pg.settlement.application.command.RerunSettlementCommand;
import com.shinhan.pg.settlement.application.port.in.GetSettlementUseCase;
import com.shinhan.pg.settlement.application.port.in.RerunSettlementUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** 개발용 API 스켈레톤. 기본/local/aws에는 등록하지 않으며 성공한 금융 결과를 생성하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
@RestController
@Profile("skeleton")
public final class SettlementController {
    private final GetSettlementUseCase getSettlementUseCase;
    private final RerunSettlementUseCase rerunSettlementUseCase;

    public SettlementController(
            GetSettlementUseCase getSettlementUseCase,
            RerunSettlementUseCase rerunSettlementUseCase) {
        this.getSettlementUseCase = java.util.Objects.requireNonNull(getSettlementUseCase);
        this.rerunSettlementUseCase = java.util.Objects.requireNonNull(rerunSettlementUseCase);
    }

    /**
     * <pre>
     * [MVP7-F04] 마감 결과·재처리
     * API 초안: GET /internal/v1/settlements/{date}
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: date
     * 출력 근거: 마감 상태, 불일치 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 불일치 시 확정 안 함
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @GetMapping("/internal/v1/settlements/{date}")
    @ResponseStatus(HttpStatus.OK)
    public GetSettlementResponse getSettlement(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getSettlementUseCase.getSettlement(
                        new GetSettlementCommand(UUID.randomUUID(), callerServiceId, date));
        return new GetSettlementResponse(
                result.cycleId(), result.businessDate(), result.status(), result.differences());
    }

    /**
     * <pre>
     * [MVP7-F04] 마감 결과·재처리
     * API 초안: POST /internal/v1/settlements/{date}/rerun
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: date
     * 출력 근거: 마감 상태, 불일치 목록
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 불일치 시 확정 안 함
     * TODO: 회차의 원래 cutoff와 입력 스냅샷을 재사용한다. 결과 대사 후에만 정산 분개·마감 확정한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @PostMapping("/internal/v1/settlements/{date}/rerun")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RerunSettlementResponse rerunSettlement(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @RequestBody RerunSettlementRequest request,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                rerunSettlementUseCase.rerunSettlement(
                        new RerunSettlementCommand(
                                UUID.randomUUID(),
                                callerServiceId,
                                request.cycleNumber(),
                                request.revision(),
                                date));
        return new RerunSettlementResponse(result.cycleId(), result.status());
    }
}

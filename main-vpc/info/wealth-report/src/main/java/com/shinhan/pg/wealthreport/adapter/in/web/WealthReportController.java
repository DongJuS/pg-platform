package com.shinhan.pg.wealthreport.adapter.in.web;

import com.shinhan.pg.wealthreport.adapter.in.web.request.GenerateReportRequest;
import com.shinhan.pg.wealthreport.adapter.in.web.response.GenerateReportResponse;
import com.shinhan.pg.wealthreport.adapter.in.web.response.GetReportResponse;
import com.shinhan.pg.wealthreport.adapter.in.web.response.ListReportsResponse;
import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.command.GetReportCommand;
import com.shinhan.pg.wealthreport.application.command.ListReportsCommand;
import com.shinhan.pg.wealthreport.application.port.in.GenerateReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.GetReportUseCase;
import com.shinhan.pg.wealthreport.application.port.in.ListReportsUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
public final class WealthReportController {
    private final GenerateReportUseCase generateReportUseCase;
    private final GetReportUseCase getReportUseCase;
    private final ListReportsUseCase listReportsUseCase;

    public WealthReportController(
            GenerateReportUseCase generateReportUseCase,
            GetReportUseCase getReportUseCase,
            ListReportsUseCase listReportsUseCase) {
        this.generateReportUseCase = java.util.Objects.requireNonNull(generateReportUseCase);
        this.getReportUseCase = java.util.Objects.requireNonNull(getReportUseCase);
        this.listReportsUseCase = java.util.Objects.requireNonNull(listReportsUseCase);
    }

    /**
     * <pre>
     * [MVP4-F01] 보고서 생성 요청
     * API 초안: POST /api/v1/wealth/reports
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 기간
     * 출력 근거: reportId, 상태(접수)
     * 정상 응답 후보: 201 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 409 진행 중 보고서 있음
     * TODO: 최신 사용자 4-A: PDF 생성·S3 저장까지 기다려 완성 결과를 동기 응답한다. 과거 202/작업 접수 응답으로 되돌리지 않는다.
     * TODO: Java 지표 계산과 기준 시점을 고정한 뒤 가명처리한다. AI는 설명을 생성하고 숫자는 계산 결과와 대조한다.
     * TODO: 요청 키의 헤더명·필수 여부와 동기 성공 200/201 세부 계약은 초안이다. timeout·동시 생성 수는 기존 설정을 실제 집행하는 코드로 연결한다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49816f8110d5e30813050b
     * </pre>
     */
    @PostMapping("/api/v1/wealth/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public GenerateReportResponse generateReport(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestBody GenerateReportRequest request,
            @RequestHeader("Idempotency-Key") String requestKey) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                generateReportUseCase.generateReport(
                        new GenerateReportCommand(
                                UUID.randomUUID(),
                                actorUserId,
                                request.from(),
                                request.to(),
                                request.accountIds(),
                                request.asOf(),
                                requestKey));
        return new GenerateReportResponse(
                result.reportId(), result.status(), result.downloadUrl(), result.completedAt());
    }

    /**
     * <pre>
     * [MVP4-F02] 보고서 결과 조회
     * API 초안: GET /api/v1/wealth/reports/{reportId}
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: reportId
     * 출력 근거: 상태, PDF 링크(만료 시각)
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403, 404
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981339cb8fae5e2cd0f23
     * </pre>
     */
    @GetMapping("/api/v1/wealth/reports/{reportId}")
    @ResponseStatus(HttpStatus.OK)
    public GetReportResponse getReport(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @PathVariable("reportId") UUID reportId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                getReportUseCase.getReport(
                        new GetReportCommand(UUID.randomUUID(), actorUserId, reportId));
        return new GetReportResponse(
                result.reportId(),
                result.status(),
                result.asOf(),
                result.downloadUrl(),
                result.urlExpiresAt());
    }

    /**
     * <pre>
     * [MVP4-F03] 보고서 목록
     * API 초안: GET /api/v1/wealth/reports
     * 상태: 초안 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 사용자 JWT / PUBLIC
     * 입력 근거: 기간, 상태
     * 출력 근거: 보고서 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 401
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981429715cd54675fcd4c
     * </pre>
     */
    @GetMapping("/api/v1/wealth/reports")
    @ResponseStatus(HttpStatus.OK)
    public ListReportsResponse listReports(
            @RequestAttribute(name = "actorUserId", required = false) UUID actorUserId,
            @RequestParam(name = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(name = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                listReportsUseCase.listReports(
                        new ListReportsCommand(
                                UUID.randomUUID(), actorUserId, from, to, status, page, size));
        return new ListReportsResponse(result.reports(), result.nextPage());
    }
}

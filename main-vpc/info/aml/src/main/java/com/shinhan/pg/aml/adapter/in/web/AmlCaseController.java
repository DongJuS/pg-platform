package com.shinhan.pg.aml.adapter.in.web;

import com.shinhan.pg.aml.adapter.in.web.response.ListAmlCasesResponse;
import com.shinhan.pg.aml.application.command.ListAmlCasesCommand;
import com.shinhan.pg.aml.application.port.in.ListAmlCasesUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
public final class AmlCaseController {
    private final ListAmlCasesUseCase listAmlCasesUseCase;

    public AmlCaseController(ListAmlCasesUseCase listAmlCasesUseCase) {
        this.listAmlCasesUseCase = java.util.Objects.requireNonNull(listAmlCasesUseCase);
    }

    /**
     * <pre>
     * [MVP6-F02] 케이스 목록 조회
     * API 초안: GET /internal/v1/aml/cases
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: 상태, 기간
     * 출력 근거: 케이스 목록
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: —
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @GetMapping("/internal/v1/aml/cases")
    @ResponseStatus(HttpStatus.OK)
    public ListAmlCasesResponse listAmlCases(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(name = "to", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                listAmlCasesUseCase.listAmlCases(
                        new ListAmlCasesCommand(
                                UUID.randomUUID(), callerServiceId, status, from, to, page, size));
        return new ListAmlCasesResponse(result.cases(), result.nextPage());
    }
}

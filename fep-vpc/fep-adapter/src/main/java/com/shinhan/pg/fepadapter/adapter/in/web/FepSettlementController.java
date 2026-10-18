package com.shinhan.pg.fepadapter.adapter.in.web;

import com.shinhan.pg.fepadapter.adapter.in.web.request.ExchangeSettlementFileRequest;
import com.shinhan.pg.fepadapter.adapter.in.web.response.ExchangeSettlementFileResponse;
import com.shinhan.pg.fepadapter.application.command.ExchangeSettlementFileCommand;
import com.shinhan.pg.fepadapter.application.port.in.ExchangeSettlementFileUseCase;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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
public final class FepSettlementController {
    private final ExchangeSettlementFileUseCase exchangeSettlementFileUseCase;

    public FepSettlementController(ExchangeSettlementFileUseCase exchangeSettlementFileUseCase) {
        this.exchangeSettlementFileUseCase =
                java.util.Objects.requireNonNull(exchangeSettlementFileUseCase);
    }

    /**
     * <pre>
     * [MVP7-F03] 정산 파일 송수신
     * API 초안: POST /internal/v1/fep/settlement/files
     * 상태: 신규 — skeleton profile의 유효 요청은 업무 미구현 501
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: 정산 파일
     * 출력 근거: 결과 파일
     * 정상 응답 후보: 202 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 무응답 시 재전송(같은 날짜 1회만 반영)
     * TODO: 같은 날짜가 아니라 cycle/version/fileKey를 중복 효과 식별자로 사용한다. 실제 고정길이 파일 필드는 미정이다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://www.figma.com/design/JOeniY2Om7ofAt39NluJXl/sinhan-financial-project?node-id=19-2
     * </pre>
     */
    @PostMapping("/internal/v1/fep/settlement/files")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExchangeSettlementFileResponse exchangeSettlementFile(
            @RequestAttribute(name = "callerServiceId", required = false) String callerServiceId,
            @RequestBody ExchangeSettlementFileRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        var result =
                exchangeSettlementFileUseCase.exchangeSettlementFile(
                        new ExchangeSettlementFileCommand(
                                UUID.randomUUID(),
                                callerServiceId,
                                request.cycleId(),
                                request.fileKey(),
                                request.checksum(),
                                request.fileContent()));
        return new ExchangeSettlementFileResponse(
                result.messageId(), result.acceptanceStatus(), result.resultReference());
    }
}

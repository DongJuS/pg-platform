package com.shinhan.pg.aigateway.adapter.in.web;

import com.shinhan.pg.aigateway.adapter.in.web.response.GetAiRequestResponse;
import com.shinhan.pg.aigateway.application.port.in.GetAiRequestUseCase;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

import java.util.UUID;

/** 보류 API 파일 자리. Spring bean·HTTP mapping을 등록하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
// 보류: @RestController
// 보류: @Profile("skeleton")
public final class AiAuditController {
    private final GetAiRequestUseCase getAiRequestUseCase;

    public AiAuditController(GetAiRequestUseCase getAiRequestUseCase) {
        this.getAiRequestUseCase = java.util.Objects.requireNonNull(getAiRequestUseCase);
    }

    /**
     * <pre>
     * [MVP3-F02] AI 요청 기록 조회
     * API 초안: GET /internal/v1/ai/requests/{requestId}
     * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
     * 인증/접근: 운영자 권한 / ADMIN
     * 입력 근거: requestId
     * 출력 근거: 처리 이력
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a49813c90a5e041a90c08df
     * </pre>
     */
    // 보류: @GetMapping("/internal/v1/ai/requests/{requestId}")
    public GetAiRequestResponse getAiRequest(String callerServiceId, UUID requestId) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        throw new FeatureNotImplementedException("MVP3-F02 (HELD)");
    }
}

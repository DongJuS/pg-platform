package com.shinhan.pg.aigateway.adapter.in.web;

import com.shinhan.pg.aigateway.adapter.in.web.request.RelayLlmRequest;
import com.shinhan.pg.aigateway.adapter.in.web.response.RelayLlmResponse;
import com.shinhan.pg.aigateway.application.port.in.RelayLlmUseCase;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** 보류 API 파일 자리. Spring bean·HTTP mapping을 등록하지 않는다.
 * 필드명·상태·인증 방식은 각 메서드의 출처/미정을 확인한다.
 */
// 보류: @RestController
// 보류: @Profile("skeleton")
public final class AiInferenceController {
    private final RelayLlmUseCase relayLlmUseCase;

    public AiInferenceController(RelayLlmUseCase relayLlmUseCase) {
        this.relayLlmUseCase = java.util.Objects.requireNonNull(relayLlmUseCase);
    }

    /**
     * <pre>
     * [MVP3-F01] AI 요청 경유
     * API 초안: POST /internal/v1/ai/inference
     * 상태: 보류 — Controller bean/HTTP mapping 등록 금지
     * 인증/접근: 서비스 인증 / INTERNAL
     * 입력 근거: 가명 처리된 프롬프트, 요청 서비스 ID
     * 출력 근거: AI 응답, requestId
     * 정상 응답 후보: 200 (실제 성공 응답은 아직 생성하지 않음)
     * 오류 후보: 403 정책 차단, 504 외부 AI 지연, 429 외부 제공사 제한. 차단 원문 저장은 확정하지 않음
     * TODO: 중계 기능과 외부 출구는 확정, /internal/v1/ai/inference wire 경로·세부 검사 계약은 보류 표기를 유지한다.
     * TODO: 검사 → 허용 목적지 확인 → LLM → 응답 검사 → 원문 없는 감사 기록. 원문 저장 요구는 확정하지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981a2b0d5c94e371b082c
     * </pre>
     */
    // 보류: @PostMapping("/internal/v1/ai/inference")
    public RelayLlmResponse relayLlm(String callerServiceId, RelayLlmRequest request) {
        // TODO: 인증 문맥 검증·요청 변환·Bean Validation은 계약 확정 후 연결한다.
        throw new FeatureNotImplementedException("MVP3-F01 (HELD)");
    }
}

package com.shinhan.pg.aigateway.application.port.in;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;
import com.shinhan.pg.aigateway.application.result.RelayLlmResult;

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
public interface RelayLlmUseCase {
    RelayLlmResult relayLlm(RelayLlmCommand command);
}

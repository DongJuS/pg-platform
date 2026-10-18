package com.shinhan.pg.aigateway.application.service;

import com.shinhan.pg.aigateway.application.command.RelayLlmCommand;
import com.shinhan.pg.aigateway.application.port.in.RelayLlmUseCase;
import com.shinhan.pg.aigateway.application.port.out.AiAuditWritePort;
import com.shinhan.pg.aigateway.application.port.out.AiRequestAuditRepository;
import com.shinhan.pg.aigateway.application.port.out.LlmRelayPort;
import com.shinhan.pg.aigateway.application.port.out.PiiInspectionPort;
import com.shinhan.pg.aigateway.application.result.RelayLlmResult;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/** 기능별 계약과 TODO를 모은 application 구현 자리.
 * Controller는 이 클래스가 구현하는 input port만 호출한다. 외부 호출을 열린 금융 DB 트랜잭션 안에서 수행하지 않는다.
 * 현재 모든 업무 메서드는 명시적 미구현 예외를 던지며 성공 DTO·가짜 금융 결과를 반환하지 않는다.
 */
public final class AiGatewayApplicationService implements RelayLlmUseCase {
    private final PiiInspectionPort piiInspectionPort;
    private final LlmRelayPort llmRelayPort;
    private final AiAuditWritePort aiAuditWritePort;
    private final AiRequestAuditRepository aiRequestAuditRepository;

    public AiGatewayApplicationService(
            PiiInspectionPort piiInspectionPort,
            LlmRelayPort llmRelayPort,
            AiAuditWritePort aiAuditWritePort,
            AiRequestAuditRepository aiRequestAuditRepository) {
        this.piiInspectionPort = java.util.Objects.requireNonNull(piiInspectionPort);
        this.llmRelayPort = java.util.Objects.requireNonNull(llmRelayPort);
        this.aiAuditWritePort = java.util.Objects.requireNonNull(aiAuditWritePort);
        this.aiRequestAuditRepository = java.util.Objects.requireNonNull(aiRequestAuditRepository);
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
     * 구현 순서:
     * 1. 신뢰한 사용자/서비스 신원과 입력의 의미를 검증한다.
     * 2. 개인정보 잔존·인젝션 검사 → AI Security Gateway가 외부 LLM 요청 중계 → 응답 검사. 기본 감사 로그는 요청 해시·사유·토큰 수를 기록하고 원문은 저장하지 않음.
     * 3. 필요한 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다. 조회는 상태를 바꾸지 않는다.
     * 4. 결과를 DTO로 반환하고 단계·실패·재시도 사실을 보존한다.
     * TODO: 중계 기능과 외부 출구는 확정, /internal/v1/ai/inference wire 경로·세부 검사 계약은 보류 표기를 유지한다.
     * TODO: 검사 → 허용 목적지 확인 → LLM → 응답 검사 → 원문 없는 감사 기록. 원문 저장 요구는 확정하지 않는다.
     * TODO[CONTRACT]: DTO 필드/필수성·성공/실패 상태·페이지네이션은 구현 전 최종 대조한다.
     * 출처: https://app.notion.com/p/3faad9292a4981a2b0d5c94e371b082c
     * </pre>
     */
    @Override
    public RelayLlmResult relayLlm(RelayLlmCommand command) {
        // TODO 1: 인증된 주체·자원 접근 범위와 입력을 확인한다.
        // TODO 2: 개인정보 잔존·인젝션 검사 → AI Security Gateway가 외부 LLM 요청 중계 → 응답 검사. 기본 감사 로그는 요청 해시·사유·토큰
        // 수를 기록하고 원문은 저장하지
        // 않음.
        // TODO 3: 외부 호출과 짧은 로컬 DB 트랜잭션을 분리한다.
        // TODO 4: 확정 결과·오류·재개 지점을 DTO와 처리 이력으로 연결한다.
        throw new FeatureNotImplementedException("MVP3-F01");
    }
}

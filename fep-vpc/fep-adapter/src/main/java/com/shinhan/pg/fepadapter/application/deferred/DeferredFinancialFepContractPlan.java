package com.shinhan.pg.fepadapter.application.deferred;

/**
 * <pre>
 * [CMN-F04] 보류 상태를 보존하는 파일 자리.
 * 금융 요청의 내부 REST 방향만 확정. PG·KoFIU·결과 callback 경로/인증/전문 계약은 상세 보류. LLM은 AI Gateway 소유다.
 * TODO: 사용자 확정 전 API annotation·Spring bean·동작을 추가하지 않는다.
 * </pre>
 */
public final class DeferredFinancialFepContractPlan {
    private DeferredFinancialFepContractPlan() {}
}

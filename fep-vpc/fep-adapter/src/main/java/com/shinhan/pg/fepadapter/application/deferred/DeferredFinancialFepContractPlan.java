package com.shinhan.pg.fepadapter.application.deferred;

/**
 *
 *
 * <pre>
 * [CMN-F04] 보류 상태를 보존하는 파일 자리.
 * PG 동기 승인·취소·환불 및 서명 callback은 ADR 0002에 따라 구현됐다. KoFIU 등 다른 MVP의 전문 계약은 계속 보류하며 LLM은 AI Gateway 소유다.
 * PG 활성 경로는 FepPgController/PgGatewayConfiguration이다. 다른 보류 계약은 활성화하지 않는다.
 * </pre>
 */
public final class DeferredFinancialFepContractPlan {
    private DeferredFinancialFepContractPlan() {}
}

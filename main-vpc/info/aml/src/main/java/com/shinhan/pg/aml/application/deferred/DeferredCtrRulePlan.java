package com.shinhan.pg.aml.application.deferred;

/**
 * <pre>
 * [MVP6-F06] 보류 상태를 보존하는 파일 자리.
 * 현금 유형과 실제 CTR 구분 정책은 보류. 일반 고액 이체를 실제 CTR 규제 준수라고 표시하지 않는다.
 * TODO: 사용자 확정 전 API annotation·Spring bean·동작을 추가하지 않는다.
 * </pre>
 */
public final class DeferredCtrRulePlan {
    private DeferredCtrRulePlan() {}
}

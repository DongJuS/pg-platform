package com.shinhan.pg.mockpg.api;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP2-F01,MVP2-F03,MVP2-F04,MVP2-F06] 승인·취소·부분 환불·조회. 중복/역순 웹훅과 응답 유실·실패 시나리오.
 * TODO: /mock/v1/pg/... HTTP 경로는 FEP/provider 계약 후 확정한다. 아래 메서드 이름은 구현 계획이다.
 * 성공·지연·오류·중복 결과를 명시적으로 선택하고 실제 금융기관/실결제/자동 자금이체를 호출하지 않는다.
 * @RestController/@Mapping/bean은 아직 등록하지 않는다.
 * </pre>
 */
public final class MockPgController {
    /** TODO: approve 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void approve(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP2-F01");
    }

    /** TODO: cancel 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void cancel(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP2-F01");
    }

    /** TODO: refund 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void refund(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP2-F01");
    }

    /** TODO: queryPayment 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void queryPayment(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP2-F01");
    }
}

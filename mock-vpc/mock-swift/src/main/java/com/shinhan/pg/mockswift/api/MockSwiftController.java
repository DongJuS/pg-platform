package com.shinhan.pg.mockswift.api;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP5-F04,MVP5-F05,MVP5-F06] 동일 UETR 전문 중복과 pacs.002 ACSC/RJCT/무응답 시나리오.
 * TODO: /mock/v1/swift/... HTTP 경로는 FEP/provider 계약 후 확정한다. 아래 메서드 이름은 구현 계획이다.
 * 성공·지연·오류·중복 결과를 명시적으로 선택하고 실제 금융기관/실결제/자동 자금이체를 호출하지 않는다.
 * @RestController/@Mapping/bean은 아직 등록하지 않는다.
 * </pre>
 */
public final class MockSwiftController {
    /** TODO: receivePacs008 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void receivePacs008(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP5-F04");
    }

    /** TODO: queryByUetr 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void queryByUetr(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP5-F04");
    }
}

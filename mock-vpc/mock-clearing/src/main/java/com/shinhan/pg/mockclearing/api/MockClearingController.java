package com.shinhan.pg.mockclearing.api;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP7-F03,MVP7-F07] 파일 건수·합계·해시 확인, 정상/불일치 결과 파일·무응답 시나리오.
 * TODO: /mock/v1/clearing/... HTTP 경로는 FEP/provider 계약 후 확정한다. 아래 메서드 이름은 구현 계획이다.
 * 성공·지연·오류·중복 결과를 명시적으로 선택하고 실제 금융기관/실결제/자동 자금이체를 호출하지 않는다.
 * @RestController/@Mapping/bean은 아직 등록하지 않는다.
 * </pre>
 */
public final class MockClearingController {
    /** TODO: receiveSettlementFile 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void receiveSettlementFile(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP7-F03");
    }

    /** TODO: queryResultFile 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void queryResultFile(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP7-F03");
    }
}

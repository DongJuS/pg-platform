package com.shinhan.pg.mockkofiu.api;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP6-F03,MVP6-F05] 동일 reportKey 접수·중복·오류 응답과 수신 확인 시나리오.
 * TODO: /mock/v1/kofiu/... HTTP 경로는 FEP/provider 계약 후 확정한다. 아래 메서드 이름은 구현 계획이다.
 * 성공·지연·오류·중복 결과를 명시적으로 선택하고 실제 금융기관/실결제/자동 자금이체를 호출하지 않는다.
 * @RestController/@Mapping/bean은 아직 등록하지 않는다.
 * </pre>
 */
public final class MockKofiuController {
    /** TODO: receiveReport 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void receiveReport(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP6-F03");
    }

    /** TODO: queryReceipt 요청 DTO·안정적 업무 키·결과·오류를 명세화한다. */
    public void queryReceipt(String syntheticPayload) {
        throw new FeatureNotImplementedException("MVP6-F03");
    }
}

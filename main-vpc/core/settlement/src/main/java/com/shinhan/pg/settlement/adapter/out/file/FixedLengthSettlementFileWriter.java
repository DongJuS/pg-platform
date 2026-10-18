package com.shinhan.pg.settlement.adapter.out.file;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

/**
 * <pre>
 * [MVP7-F02] TODO: 기관별 고정길이 파일 인코딩·필드 폭·금액 표현·건수/합계/해시를 확정한다.
 * 생성 파일은 cycle/version/fileKey에 연결한다. 수수료/환율 산식을 임의 확정하지 않는다.
 * </pre>
 */
public final class FixedLengthSettlementFileWriter {
    public byte[] writeValidatedFile(String validatedNettingData) {
        throw new FeatureNotImplementedException("MVP7-F02");
    }
}

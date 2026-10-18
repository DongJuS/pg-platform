package com.shinhan.pg.wealthreport.adapter.out.tokenization;

import com.shinhan.pg.wealthreport.application.port.out.PseudonymizationPort;
import com.shinhan.pg.wealthreport.application.readmodel.ReportSnapshot;

/** 이름·계좌번호 토큰화 및 모듈 내부 매핑 저장 위치. 외부 LLM에 원본을 보내지 않는다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class PseudonymizationSkeleton implements PseudonymizationPort {
    @Override
    public String tokenize(ReportSnapshot snapshot) {
        throw new UnsupportedOperationException("SKELETON wealth-report: tokenize");
    }
}

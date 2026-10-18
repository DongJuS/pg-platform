package com.shinhan.pg.wealthreport.application.port.out;

import com.shinhan.pg.wealthreport.application.readmodel.ReportSnapshot;

/** 이름·계좌번호 토큰화 및 모듈 내부 매핑 저장 위치. 외부 LLM에 원본을 보내지 않는다. */
// TODO: 검증된 초안의 토큰 복원을 TokenMappingRepository와 연결하는 메서드를 추가한다. 복원은 내부망에서만 수행하고 원본을 LLM에 보내지 않는다.
public interface PseudonymizationPort {
    String tokenize(ReportSnapshot snapshot);
}

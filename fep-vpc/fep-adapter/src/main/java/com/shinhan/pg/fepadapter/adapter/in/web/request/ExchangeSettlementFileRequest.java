package com.shinhan.pg.fepadapter.adapter.in.web.request;

import java.util.UUID;

/** [MVP7-F03] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record ExchangeSettlementFileRequest(
        UUID cycleId, String fileKey, String checksum, String fileContent) {
    @Override
    public String toString() {
        return "ExchangeSettlementFileRequest[REDACTED]";
    }
}

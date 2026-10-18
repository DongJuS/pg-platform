package com.shinhan.pg.fepadapter.adapter.in.web.request;

import java.util.UUID;

/** [MVP5-F04] 요청 DTO 초안. 신원/멱등키/경로 변수는 본문과 구분한다. 검증 제약은 주석의 계약을 따라 추가한다. */
public record SendSwiftMessageRequest(
        UUID transferId, String uetr, String messageKey, String pacs008) {
    @Override
    public String toString() {
        return "SendSwiftMessageRequest[REDACTED]";
    }
}

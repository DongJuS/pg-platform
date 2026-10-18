package com.shinhan.pg.fepadapter.adapter.in.web.response;

import java.util.UUID;

/** [MVP5-F04] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record SendSwiftMessageResponse(UUID messageId, String acceptanceStatus) {}

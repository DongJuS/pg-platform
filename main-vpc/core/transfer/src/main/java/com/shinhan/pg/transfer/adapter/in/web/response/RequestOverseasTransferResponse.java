package com.shinhan.pg.transfer.adapter.in.web.response;

import java.util.UUID;

/** [MVP5-F01] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RequestOverseasTransferResponse(UUID transferId, String uetr, String status) {}

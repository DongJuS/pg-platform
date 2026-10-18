package com.shinhan.pg.transfer.adapter.in.web.response;

import java.util.UUID;

/** [MVP1-F15] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record RequestTransferResponse(UUID transferId, UUID transactionId, String status) {}

package com.shinhan.pg.transfer.adapter.in.web.response;

import java.util.UUID;

/** [MVP5-F02] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetOverseasTransferResponse(
        UUID transferId, String uetr, String status, String rejectionReason) {}

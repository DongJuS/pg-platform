package com.shinhan.pg.transfer.adapter.in.web.response;

import java.util.UUID;

/**
 * [MVP1-F15] 409 중복 요청의 오류 응답. 공통 ErrorResponse(code/message/traceId)에
 * 기존 이체 식별자를 detail로 포함한다(WO-A6 계약: 같은 키 재요청은 409 + 기존 transferId 노출).
 */
public record DuplicateTransferErrorResponse(
        String code, String message, String traceId, UUID existingTransferId) {}

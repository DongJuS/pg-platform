package com.shinhan.pg.transfer.adapter.in.web.response;

import com.shinhan.pg.transfer.application.readmodel.TransferHistoryView;

import java.util.List;
import java.util.UUID;

/** [MVP1-F16] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetTransferResponse(
        UUID transferId, UUID transactionId, String status, List<TransferHistoryView> history) {}

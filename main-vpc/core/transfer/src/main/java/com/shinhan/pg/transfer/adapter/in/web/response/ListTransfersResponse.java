package com.shinhan.pg.transfer.adapter.in.web.response;

import com.shinhan.pg.transfer.application.readmodel.TransferSummary;

import java.util.List;

/** [MVP1-F17] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ListTransfersResponse(List<TransferSummary> transfers, Integer nextPage) {}

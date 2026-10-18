package com.shinhan.pg.payment.adapter.in.web.response;

import com.shinhan.pg.payment.application.readmodel.RefundSummary;

import java.util.List;

/** [MVP2-F05] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record ListRefundsResponse(List<RefundSummary> refunds) {}

package com.shinhan.pg.payment.adapter.in.web.response;

import com.shinhan.pg.payment.application.readmodel.ReconciliationDifferenceView;

import java.util.List;
import java.util.UUID;

/** [MVP2-F09] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetReconciliationResponse(
        UUID runId, String status, List<ReconciliationDifferenceView> differences) {}

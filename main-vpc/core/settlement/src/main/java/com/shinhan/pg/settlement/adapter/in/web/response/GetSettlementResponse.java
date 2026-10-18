package com.shinhan.pg.settlement.adapter.in.web.response;

import com.shinhan.pg.settlement.application.readmodel.SettlementDifferenceView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** [MVP7-F04] 응답 DTO 초안. DB Entity를 직접 반환하지 않는다. */
public record GetSettlementResponse(
        UUID cycleId,
        LocalDate businessDate,
        String status,
        List<SettlementDifferenceView> differences) {}

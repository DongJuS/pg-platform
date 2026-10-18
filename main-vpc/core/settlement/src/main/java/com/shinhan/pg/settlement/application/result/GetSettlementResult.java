package com.shinhan.pg.settlement.application.result;

import com.shinhan.pg.settlement.application.readmodel.SettlementDifferenceView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** [MVP7-F04] 유스케이스 결과 초안. 업무 구현 완료 뒤에만 생성한다. HTTP/JSON 필수 필드는 최종 계약 전이다. */
public record GetSettlementResult(
        UUID cycleId,
        LocalDate businessDate,
        String status,
        List<SettlementDifferenceView> differences) {}

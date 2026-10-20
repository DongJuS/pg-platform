package com.shinhan.pg.wealthreport.application.port.out;

/**
 * Optional internal preparation check only. Readiness never authorizes external transfer or creates
 * a draft.
 */
public interface AiPreparationPort {
    void checkAggregateText(String aggregateText);
}

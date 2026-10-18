package com.shinhan.pg.fepadapter.application.port.in;

import com.shinhan.pg.fepadapter.application.command.QueryInstitutionResultCommand;
import com.shinhan.pg.fepadapter.application.result.QueryInstitutionResultResult;

/** 내부 REST 조회. 무응답 요청을 새 금융 거래로 바꾸지 않고 같은 messageKey로 결과를 확인한다. */
public interface QueryInstitutionResultUseCase {
    QueryInstitutionResultResult queryInstitutionResult(QueryInstitutionResultCommand command);
}

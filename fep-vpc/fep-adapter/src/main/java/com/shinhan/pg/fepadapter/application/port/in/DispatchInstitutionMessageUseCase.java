package com.shinhan.pg.fepadapter.application.port.in;

import com.shinhan.pg.fepadapter.application.command.DispatchInstitutionMessageCommand;
import com.shinhan.pg.fepadapter.application.result.DispatchInstitutionMessageResult;

/** 내부 REST 금융 대외 전문 접수. 기관 최종 결과를 즉시 접수 성공과 구분한다. */
public interface DispatchInstitutionMessageUseCase {
    DispatchInstitutionMessageResult dispatchInstitutionMessage(
            DispatchInstitutionMessageCommand command);
}

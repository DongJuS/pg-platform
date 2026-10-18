package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.application.command.DispatchInstitutionMessageCommand;
import com.shinhan.pg.fepadapter.application.result.DispatchInstitutionMessageResult;

/** Pg Mock 기관의 전문 변환·송수신 위치. 내부 접수와 기관 최종 결과를 구분한다. */
public interface PgInstitutionPort {
    DispatchInstitutionMessageResult send(DispatchInstitutionMessageCommand command);
}

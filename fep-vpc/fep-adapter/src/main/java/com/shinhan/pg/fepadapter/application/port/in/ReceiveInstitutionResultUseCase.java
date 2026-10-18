package com.shinhan.pg.fepadapter.application.port.in;

import com.shinhan.pg.fepadapter.application.command.ReceiveInstitutionResultCommand;

/** 기관 callback/result 수신 위치. 구체 인증·반환·전파 계약은 후속이다. */
public interface ReceiveInstitutionResultUseCase {
    void receiveInstitutionResult(ReceiveInstitutionResultCommand command);
}

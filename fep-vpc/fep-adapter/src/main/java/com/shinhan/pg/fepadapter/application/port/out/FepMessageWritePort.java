package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.fepadapter.application.command.DispatchInstitutionMessageCommand;
import com.shinhan.pg.fepadapter.application.command.ReceiveInstitutionResultCommand;

/** channelCode+messageKey로 중복 전송을 방지하고 전문·전송 시도·결과를 저장할 위치. */
public interface FepMessageWritePort {
    void recordAccepted(DispatchInstitutionMessageCommand command);

    void recordResult(ReceiveInstitutionResultCommand command);
}

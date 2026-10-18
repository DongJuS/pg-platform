package com.shinhan.pg.fepadapter.adapter.out.persistence;

import com.shinhan.pg.fepadapter.application.command.DispatchInstitutionMessageCommand;
import com.shinhan.pg.fepadapter.application.command.ReceiveInstitutionResultCommand;
import com.shinhan.pg.fepadapter.application.port.out.FepMessageWritePort;

/**
 * channelCode+messageKey로 중복 전송을 방지하고 전문·전송 시도·결과를 저장할 위치. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class FepMessageWriteSkeleton implements FepMessageWritePort {
    @Override
    public void recordAccepted(DispatchInstitutionMessageCommand command) {
        throw new UnsupportedOperationException("SKELETON fep-adapter: recordAccepted");
    }

    @Override
    public void recordResult(ReceiveInstitutionResultCommand command) {
        throw new UnsupportedOperationException("SKELETON fep-adapter: recordResult");
    }
}

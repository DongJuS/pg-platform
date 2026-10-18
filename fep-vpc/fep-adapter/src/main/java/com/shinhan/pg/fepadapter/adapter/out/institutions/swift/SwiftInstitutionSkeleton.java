package com.shinhan.pg.fepadapter.adapter.out.institutions.swift;

import com.shinhan.pg.fepadapter.application.command.DispatchInstitutionMessageCommand;
import com.shinhan.pg.fepadapter.application.port.out.SwiftInstitutionPort;
import com.shinhan.pg.fepadapter.application.result.DispatchInstitutionMessageResult;

/** Swift Mock 기관의 전문 변환·송수신 위치. 내부 접수와 기관 최종 결과를 구분한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class SwiftInstitutionSkeleton implements SwiftInstitutionPort {
    @Override
    public DispatchInstitutionMessageResult send(DispatchInstitutionMessageCommand command) {
        throw new UnsupportedOperationException("SKELETON fep-adapter: send");
    }
}

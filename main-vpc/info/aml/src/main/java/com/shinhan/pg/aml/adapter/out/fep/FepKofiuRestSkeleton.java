package com.shinhan.pg.aml.adapter.out.fep;

import com.shinhan.pg.aml.application.command.AmlReportDraft;
import com.shinhan.pg.aml.application.port.out.FepKofiuPort;

import java.util.UUID;

/**
 * 자동 판정한 KoFIU Mock 보고를 내부 REST → FEP로 전송한다. 전송 실패는 보고 확정 취소가 아니라 재시도 대상이다. 실제 처리는 미구현이며
 * DB·HTTP·Kafka에 연결하지 않는다.
 */
public final class FepKofiuRestSkeleton implements FepKofiuPort {
    @Override
    public UUID submitReport(AmlReportDraft report) {
        throw new UnsupportedOperationException("SKELETON aml: submitReport");
    }
}

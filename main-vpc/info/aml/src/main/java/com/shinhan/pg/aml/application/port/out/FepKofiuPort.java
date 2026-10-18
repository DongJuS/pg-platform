package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.aml.application.command.AmlReportDraft;

import java.util.UUID;

/** 자동 판정한 KoFIU Mock 보고를 내부 REST → FEP로 전송한다. 전송 실패는 보고 확정 취소가 아니라 재시도 대상이다. */
public interface FepKofiuPort {
    UUID submitReport(AmlReportDraft report);
}

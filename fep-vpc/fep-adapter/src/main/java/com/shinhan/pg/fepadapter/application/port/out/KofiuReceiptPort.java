package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;

import java.util.UUID;

public interface KofiuReceiptPort {
    KofiuResult register(AmlReportDocument document);

    KofiuResult get(String key);

    AmlReportDocument document(String key);

    boolean claim(String key, UUID worker, boolean retry, int maximum, long interval);

    void finish(String key, UUID worker, KofiuResult result);

    void purgeExpired();

    void observe(String key, KofiuResult result);
}

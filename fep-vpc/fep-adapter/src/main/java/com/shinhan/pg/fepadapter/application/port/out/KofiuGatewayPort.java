package com.shinhan.pg.fepadapter.application.port.out;

import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;

public interface KofiuGatewayPort {
    KofiuResult send(AmlReportDocument document);

    KofiuResult query(AmlReportDocument document);
}

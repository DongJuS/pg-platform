package com.shinhan.pg.aml.application.port.out;

import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;

public interface FepKofiuPort {
    KofiuResult submit(AmlReportDocument report);

    KofiuResult query(AmlReportDocument report);

    KofiuResult retransmit(AmlReportDocument report);
}

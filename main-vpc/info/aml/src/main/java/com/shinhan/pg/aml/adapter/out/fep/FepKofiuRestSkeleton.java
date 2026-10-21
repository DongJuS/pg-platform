package com.shinhan.pg.aml.adapter.out.fep;

import com.shinhan.pg.aml.application.port.out.FepKofiuPort;
import com.shinhan.pg.common.amlcontracts.AmlReportDocument;
import com.shinhan.pg.common.amlcontracts.KofiuResult;
import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;

public final class FepKofiuRestSkeleton implements FepKofiuPort {
    public KofiuResult submit(AmlReportDocument d) {
        throw new FeatureNotImplementedException("MVP6-F03");
    }

    public KofiuResult query(AmlReportDocument d) {
        throw new FeatureNotImplementedException("MVP6-F05");
    }

    public KofiuResult retransmit(AmlReportDocument d) {
        throw new FeatureNotImplementedException("MVP6-F05");
    }
}

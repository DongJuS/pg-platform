package com.shinhan.pg.wealthreport.domain.error;

/** Only fixed codes cross the boundary: never retain raw client, SQL or AI exception bodies. */
public final class ReportException extends RuntimeException {
    private final WealthReportErrorCode code;

    public ReportException(WealthReportErrorCode code) {
        super("REPORT-" + code);
        this.code = code;
    }

    public WealthReportErrorCode code() {
        return code;
    }
}

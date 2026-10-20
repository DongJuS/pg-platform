package com.shinhan.pg.swift.application.readmodel;

import java.util.List;

/** Transmission uncertainty and institution outcome are separate dimensions. */
public record SwiftQueryResult(List<String> reports, boolean transmissionUnknown) {
    public SwiftQueryResult {
        reports = List.copyOf(reports);
    }

    @Override
    public String toString() {
        return "SwiftQueryResult[REDACTED]";
    }
}

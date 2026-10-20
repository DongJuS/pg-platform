package com.shinhan.pg.wealthreport.adapter.out.storage;

import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;

import java.net.URI;
import java.util.UUID;

/** Unregistered S3 placeholder. No bucket creation or public download URL is authorized. */
public final class ReportStorageSkeleton implements ReportStoragePort {
    public URI complete(UUID id, byte[] pdf, String source) {
        throw new IllegalStateException("S3 not configured");
    }

    public byte[] read(UUID id) {
        throw new IllegalStateException("S3 not configured");
    }
}

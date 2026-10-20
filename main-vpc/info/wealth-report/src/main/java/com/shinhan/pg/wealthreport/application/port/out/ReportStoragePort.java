package com.shinhan.pg.wealthreport.application.port.out;

import java.net.URI;
import java.util.UUID;

/**
 * Development private storage. Completion and PDF bytes commit atomically in report DB. Production
 * S3 requires its own adapter and retention decision; no public bearer URL.
 */
public interface ReportStoragePort {
    URI complete(UUID reportId, byte[] pdf, String draftSource);

    byte[] read(UUID reportId);
}

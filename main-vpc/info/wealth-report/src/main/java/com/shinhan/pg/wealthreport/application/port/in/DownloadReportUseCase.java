package com.shinhan.pg.wealthreport.application.port.in;

import java.util.UUID;

public interface DownloadReportUseCase {
    byte[] download(UUID actorUserId, UUID reportId);
}

package com.shinhan.pg.wealthreport.adapter.out.storage;

import com.shinhan.pg.wealthreport.application.port.out.ReportStoragePort;

import java.net.URI;
import java.util.UUID;

/** 비공개 S3 저장과 제한 시간 다운로드 URL 생성 위치. 성공 결과는 PDF 저장 후 반환한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class ReportStorageSkeleton implements ReportStoragePort {
    @Override
    public URI storeAndCreateDownloadUrl(UUID reportId, byte[] pdf) {
        throw new UnsupportedOperationException(
                "SKELETON wealth-report: storeAndCreateDownloadUrl");
    }
}

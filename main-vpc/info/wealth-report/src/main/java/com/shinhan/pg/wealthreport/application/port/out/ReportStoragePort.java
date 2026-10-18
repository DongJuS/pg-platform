package com.shinhan.pg.wealthreport.application.port.out;

import java.net.URI;
import java.util.UUID;

/** 비공개 S3 저장과 제한 시간 다운로드 URL 생성 위치. 성공 결과는 PDF 저장 후 반환한다. */
public interface ReportStoragePort {
    URI storeAndCreateDownloadUrl(UUID reportId, byte[] pdf);
}

package com.shinhan.pg.wealthreport.adapter.out.pdf;

import com.shinhan.pg.wealthreport.application.port.out.PdfRenderingPort;

/** 초안 검증·내부 토큰 복원 뒤 PDF를 생성할 위치. 외부 통신 여부는 렌더러 선택 후 정한다. 실제 처리는 미구현이며 DB·HTTP·Kafka에 연결하지 않는다. */
public final class PdfRenderingSkeleton implements PdfRenderingPort {
    @Override
    public byte[] render(String verifiedReport) {
        throw new UnsupportedOperationException("SKELETON wealth-report: render");
    }
}

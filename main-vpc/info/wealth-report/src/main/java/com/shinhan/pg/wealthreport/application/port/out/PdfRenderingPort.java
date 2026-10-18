package com.shinhan.pg.wealthreport.application.port.out;

/** 초안 검증·내부 토큰 복원 뒤 PDF를 생성할 위치. 외부 통신 여부는 렌더러 선택 후 정한다. */
public interface PdfRenderingPort {
    byte[] render(String verifiedReport);
}

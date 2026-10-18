package com.shinhan.pg.common.commonerror;

/** 개발 스켈레톤의 명시적 미구현 표시. HTTP 변환은 웹 계층에서 하며 금융 성공을 뜻하지 않는다. */
public final class FeatureNotImplementedException extends UnsupportedOperationException {
    public FeatureNotImplementedException(String featureId) {
        super("Unimplemented feature: " + featureId);
    }
}

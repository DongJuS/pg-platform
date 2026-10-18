package com.shinhan.pg.swift.adapter.in.web;

import com.shinhan.pg.common.commonerror.FeatureNotImplementedException;
import com.shinhan.pg.swift.domain.error.SwiftErrorCode;

/**
 * <pre>
 * TODO: SwiftErrorCode를 메서드 주석의 400/401/403/404/409/422/429/5xx 계약과 연결한다.
 * 업무 오류를 성공 DTO로 바꾸지 않는다. 현재 미구현 endpoint는 공통 handler가 501로 표시한다.
 * </pre>
 */
public final class SwiftErrorMapper {
    public org.springframework.http.HttpStatus status(SwiftErrorCode error) {
        throw new FeatureNotImplementedException("swift-ERROR-CONTRACT");
    }
}

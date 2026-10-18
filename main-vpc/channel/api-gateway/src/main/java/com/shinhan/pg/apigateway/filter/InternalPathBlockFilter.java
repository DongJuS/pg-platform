package com.shinhan.pg.apigateway.filter;

/**
 * <pre>
 * [CMN-F02] 망분리·접근 통제 — 내부 경로 차단 필터 구현 자리.
 * TODO: 내부 경로·서비스 인증 경계·경로 인코딩/우회 검증. 외부 catch-all 라우팅으로 내부 API를 노출하지 않는다.
 * TODO: 구현·검증 후 GlobalFilter bean으로 조립한다. 현재 등록되지 않은 스켈레톤이다.
 * </pre>
 */
public final class InternalPathBlockFilter {
    private InternalPathBlockFilter() {}
}

package com.shinhan.pg.apigateway.filter;

/**
 * <pre>
 * [CMN-F02] 망분리·접근 통제 — 신원 검증 필터 구현 자리.
 * TODO: RS256 공개키 검증·만료/issuer/audience 확인·철회 jti 검사 후 신뢰한 actor 문맥 전달. 실제 키/JWKS/정책은 후속이다.
 * TODO: 구현·검증 후 GlobalFilter bean으로 조립한다. 현재 등록되지 않은 스켈레톤이다.
 * </pre>
 */
public final class GatewayJwtFilter {
    private GatewayJwtFilter() {}
}

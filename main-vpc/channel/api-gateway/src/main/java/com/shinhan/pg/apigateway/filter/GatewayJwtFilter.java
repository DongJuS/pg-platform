package com.shinhan.pg.apigateway.filter;

/**
 * <pre>
 * [CMN-F02] 망분리·접근 통제 — 신원 검증 필터 구현 자리.
 * TODO: RS256 공개키 검증·만료/issuer/audience 확인·철회 jti 검사 후 신뢰한 actor 문맥 전달. 실제 키/JWKS/정책은 후속이다.
 * TODO: 구현·검증 후 GlobalFilter bean으로 조립한다. 현재 등록되지 않은 스켈레톤이다.
 * </pre>
 *
 * 구현 상태(WO-A2, 단일 필터 방향): 신원 검증의 실구현은 별도 GlobalFilter가 아니라
 * Spring Security 리소스서버에 있다 — GatewayJwtConfiguration(JWKS decoder + issuer/audience
 * validator)과 GatewaySecurityConfiguration(oauth2ResourceServer 연결)을 참조.
 * 검증된 신원의 downstream 전달(X-User-Id)과 외부 신원 헤더 제거는 IdentityHeaderFilter가 담당한다.
 * 철회 jti 검사·키 로테이션 정책은 위 TODO 그대로 미정으로 남긴다.
 * 이 클래스에 중복 JWT 필터를 다시 추가하지 않는다.
 */
public final class GatewayJwtFilter {
    private GatewayJwtFilter() {}
}

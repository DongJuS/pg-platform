package com.shinhan.pg.auth.application.port.out;

import java.util.List;

/**
 * [CMN-F02] JWKS 조회 포트. 웹 어댑터가 서명 키 어댑터의 공개키 JWK Set을 조회한다.
 * RFC 7517 JWK Set 모델 — 공개키 파라미터(kty/use/alg/kid/n/e)만 담고 개인키는 포함하지 않는다.
 */
public interface JwksQueryPort {
    JwkSet jwkSet();

    record JwkSet(List<Jwk> keys) {}

    record Jwk(String kty, String use, String alg, String kid, String n, String e) {}
}

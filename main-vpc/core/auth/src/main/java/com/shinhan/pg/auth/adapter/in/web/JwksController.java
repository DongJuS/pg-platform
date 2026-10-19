package com.shinhan.pg.auth.adapter.in.web;

import com.shinhan.pg.auth.application.port.out.JwksQueryPort;
import com.shinhan.pg.auth.application.port.out.JwksQueryPort.JwkSet;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * [CMN-F02] JWKS 엔드포인트. gateway 리소스서버가 jwk-set-uri로 소비한다.
 * 응답은 RFC 7517 JWK Set — 공개키(n/e/kid/kty/alg/use)만 노출하고 개인키는 절대 포함하지 않는다.
 * 인증/접근: 없음 / PUBLIC (공개키 메타데이터)
 * 기존 auth 컨트롤러와 동일하게 skeleton 프로필에만 등록한다(프로필 승격은 별도 결정 사항).
 */
@RestController
@Profile("skeleton")
public final class JwksController {
    private final JwksQueryPort jwksQueryPort;

    public JwksController(JwksQueryPort jwksQueryPort) {
        this.jwksQueryPort = java.util.Objects.requireNonNull(jwksQueryPort);
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwkSet> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(300, TimeUnit.SECONDS).cachePublic())
                .body(jwksQueryPort.jwkSet());
    }
}

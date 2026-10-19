package com.shinhan.pg.auth.adapter.out.security;

import com.shinhan.pg.auth.application.port.out.JwksQueryPort;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * [CMN-F02] RS256 서명 키의 공개키를 JWK Set으로 노출하는 조회 어댑터.
 * WO-A1은 AuthSkeletonConfiguration 수정을 허용하지 않으므로(기존 파일 수정은
 * Rs256JwtSupport 접근자 1건만 허용) 컴포넌트 스캔으로 bean을 등록한다.
 * TODO[CONFIG]: skeleton bean 조립을 config로 일원화하는 후속 정리 시 이 어댑터도
 * AuthSkeletonConfiguration @Bean 등록 방식으로 이전할지 함께 결정한다.
 */
@Component
@Profile("skeleton")
public final class JwksAdapter implements JwksQueryPort {
    private final Rs256JwtSupport jwt;

    public JwksAdapter(Rs256JwtSupport jwt) {
        this.jwt = java.util.Objects.requireNonNull(jwt);
    }

    @Override
    public JwkSet jwkSet() {
        return JwksSupport.jwkSet(jwt.publicKey(), jwt.keyId());
    }
}

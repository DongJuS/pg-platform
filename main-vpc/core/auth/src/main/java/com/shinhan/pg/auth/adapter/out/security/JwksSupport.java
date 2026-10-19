package com.shinhan.pg.auth.adapter.out.security;

import com.shinhan.pg.auth.application.port.out.JwksQueryPort;
import com.shinhan.pg.auth.application.port.out.JwksQueryPort.Jwk;
import com.shinhan.pg.auth.application.port.out.JwksQueryPort.JwkSet;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * RSA 공개키를 RFC 7517 JWK Set 모델로 변환한다.
 * n/e는 부호 없는 big-endian 옥텟을 base64url(무패딩)로 인코딩한다(RFC 7518 §6.3.1).
 * 개인키 파라미터(d/p/q 등)는 어떤 경로로도 포함하지 않는다.
 * 키 로테이션(다중 키)은 미정 정책 — 현재는 단일 키만 노출한다.
 */
public final class JwksSupport {
    private JwksSupport() {}

    public static JwkSet jwkSet(RSAPublicKey publicKey, String keyId) {
        var jwk = new Jwk(
                "RSA",
                "sig",
                "RS256",
                keyId,
                base64UrlUnsigned(publicKey.getModulus()),
                base64UrlUnsigned(publicKey.getPublicExponent()));
        return new JwkSet(List.of(jwk));
    }

    private static String base64UrlUnsigned(BigInteger value) {
        var bytes = value.toByteArray();
        // BigInteger.toByteArray()는 부호 비트용 선행 0x00을 붙일 수 있으므로 제거한다.
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

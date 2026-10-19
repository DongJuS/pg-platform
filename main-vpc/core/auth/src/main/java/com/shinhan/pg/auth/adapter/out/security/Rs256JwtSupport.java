package com.shinhan.pg.auth.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * [MVP1-F02,MVP1-F03] JDK 기반 RS256 JWT 발급·검증. jjwt 등 외부 라이브러리 없이
 * JWS compact 직렬화와 SHA256withRSA 서명을 구현한다.
 * 토큰 원문·키 원문을 로그에 남기지 않는다.
 */
public final class Rs256JwtSupport {
    private final PrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    public Rs256JwtSupport(PrivateKey privateKey, RSAPublicKey publicKey, String keyId) {
        this.privateKey = java.util.Objects.requireNonNull(privateKey);
        this.publicKey = java.util.Objects.requireNonNull(publicKey);
        this.keyId = java.util.Objects.requireNonNull(keyId);
    }

    /** JWKS 노출용 공개키 접근자. 개인키는 절대 반환하지 않는다. */
    public RSAPublicKey publicKey() {
        return publicKey;
    }

    /** JWKS 노출용 key id 접근자. 발급 토큰 header의 kid와 동일 값이다. */
    public String keyId() {
        return keyId;
    }

    /** 개발용 임시 키쌍 생성. 서버 재시작 시 이전 토큰이 전부 무효화되므로 프로필 기본값으로만 사용한다. */
    public static Rs256JwtSupport generateEphemeral(String keyId) {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new Rs256JwtSupport(
                    pair.getPrivate(), (RSAPublicKey) pair.getPublic(), keyId);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("RSA key generation failed", error);
        }
    }

    /** PKCS#8 PEM("-----BEGIN PRIVATE KEY-----")과 X.509 PEM을 읽는다. */
    public static Rs256JwtSupport fromPem(String privateKeyPem, String publicKeyPem, String keyId) {
        try {
            var privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodePem(privateKeyPem, "PRIVATE KEY")));
            var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodePem(publicKeyPem, "PUBLIC KEY")));
            return new Rs256JwtSupport(privateKey, publicKey, keyId);
        } catch (GeneralSecurityException error) {
            throw new IllegalArgumentException("invalid JWT key PEM", error);
        }
    }

    private static byte[] decodePem(String pem, String label) {
        var body = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    public String issue(UUID userId, String issuer, String audience, DurationSpec lifetime) {
        var now = Instant.now();
        var header = Map.<String, Object>of("alg", "RS256", "typ", "JWT", "kid", keyId);
        var claims = new LinkedHashMap<String, Object>();
        claims.put("iss", issuer);
        claims.put("aud", audience);
        claims.put("sub", userId.toString());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plus(lifetime.duration()).getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        return sign(header, claims);
    }

    private String sign(Map<String, Object> header, Map<String, Object> claims) {
        try {
            var headerPart = base64Url(json(header).getBytes(StandardCharsets.US_ASCII));
            var payloadPart = base64Url(json(claims).getBytes(StandardCharsets.US_ASCII));
            var signingInput = (headerPart + "." + payloadPart).getBytes(StandardCharsets.US_ASCII);
            var signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(signingInput);
            var signature = signer.sign();
            return headerPart + "." + payloadPart + "." + base64Url(signature);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("JWT signing failed", error);
        }
    }

    /** 서명·만료·issuer·audience를 검증하고 주요 claim을 반환한다. 실패 시 null을 반환하지 않고 예외를 던진다. */
    public VerifiedToken verify(String token, String expectedIssuer, String expectedAudience) {
        var parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("malformed JWT");
        }
        try {
            var signingInput = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
            var signature = Base64.getUrlDecoder().decode(parts[2]);
            var verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(signingInput);
            if (!verifier.verify(signature)) {
                throw new SecurityException("JWT signature mismatch");
            }
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("JWT verification failed", error);
        }
        var claims = decodeClaims(parts[1]);
        var issuer = (String) claims.get("iss");
        var audience = (String) claims.get("aud");
        if (!expectedIssuer.equals(issuer) || !expectedAudience.equals(audience)) {
            throw new SecurityException("JWT issuer/audience mismatch");
        }
        var now = Instant.now().getEpochSecond();
        var exp = ((Number) claims.get("exp")).longValue();
        if (now >= exp) {
            throw new SecurityException("JWT expired");
        }
        return new VerifiedToken(
                UUID.fromString((String) claims.get("sub")),
                (String) claims.get("jti"),
                Instant.ofEpochSecond(exp));
    }

    /** 서명 없이 claim을 읽는다. 로그아웃처럼 이미 신뢰한 토큰의 jti/만료만 필요한 경우에 한한다. */
    public UnverifiedClaims parseUnverified(String token) {
        var parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("malformed JWT");
        }
        var claims = decodeClaims(parts[1]);
        return new UnverifiedClaims(
                (String) claims.get("jti"),
                Instant.ofEpochSecond(((Number) claims.get("exp")).longValue()));
    }

    private Map<String, Object> decodeClaims(String payloadPart) {
        var json = new String(Base64.getUrlDecoder().decode(payloadPart), StandardCharsets.UTF_8);
        var claims = new LinkedHashMap<String, Object>();
        for (var pair : json.substring(1, json.length() - 1).split(",")) {
            var kv = pair.split(":", 2);
            var key = kv[0].trim().replace("\"", "");
            var raw = kv[1].trim();
            if (raw.startsWith("\"")) {
                claims.put(key, raw.substring(1, raw.length() - 1));
            } else {
                claims.put(key, Long.parseLong(raw));
            }
        }
        return claims;
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String json(Map<String, Object> values) {
        var out = new StringBuilder("{");
        var first = true;
        for (var entry : values.entrySet()) {
            if (!first) out.append(',');
            out.append('"').append(entry.getKey()).append('"').append(':');
            var value = entry.getValue();
            if (value instanceof String text) {
                out.append('"').append(text).append('"');
            } else {
                out.append(value);
            }
            first = false;
        }
        return out.append('}').toString();
    }

    /** 토큰 수명 명세. record 이름 충돌을 피하기 위한 래퍼. */
    public record DurationSpec(java.time.Duration duration) {}

    public record VerifiedToken(UUID userId, String jti, Instant expiresAt) {}

    public record UnverifiedClaims(String jti, Instant expiresAt) {}
}

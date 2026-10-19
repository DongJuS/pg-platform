package com.shinhan.pg.apigateway.config;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * CMN-F02 JWT validator 계약 고정(WO-A2): 만료 + issuer(pg-platform-auth) + audience(pg-platform) 검증.
 * standalone — JWKS 조회 없이 validator만 직접 검증한다(서명 검증은 e2e 웨이브에서 실 JWKS로 확인).
 */
class GatewayJwtConfigurationTest {
    private static final GatewayJwtProperties PROPS=new GatewayJwtProperties("pg-platform-auth","pg-platform");
    private static Jwt jwt(String issuer,List<String> audience,Instant expiresAt) {
        var builder=Jwt.withTokenValue("token")
            .header("alg","RS256")
            .subject("user-1")
            .issuedAt(expiresAt.minusSeconds(600))
            .expiresAt(expiresAt);
        if (issuer!=null) builder.issuer(issuer);
        if (audience!=null) builder.audience(audience);
        return builder.build();
    }
    private static boolean valid(Jwt jwt) {
        return GatewayJwtConfiguration.jwtValidator(PROPS).validate(jwt).getErrors().isEmpty();
    }
    @Test void tokenWithContractIssuerAndAudiencePasses() {
        assertTrue(valid(jwt("pg-platform-auth",List.of("pg-platform"),Instant.now().plusSeconds(300))));
    }
    @Test void wrongIssuerFails() {
        assertFalse(valid(jwt("someone-else",List.of("pg-platform"),Instant.now().plusSeconds(300))));
    }
    @Test void wrongAudienceFails() {
        assertFalse(valid(jwt("pg-platform-auth",List.of("other-service"),Instant.now().plusSeconds(300))));
    }
    @Test void missingAudienceFails() {
        assertFalse(valid(jwt("pg-platform-auth",null,Instant.now().plusSeconds(300))));
    }
    @Test void expiredTokenFails() {
        assertFalse(valid(jwt("pg-platform-auth",List.of("pg-platform"),Instant.now().minusSeconds(60))));
    }
}

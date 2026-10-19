package com.shinhan.pg.auth.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.auth.adapter.out.security.JwksAdapter;
import com.shinhan.pg.auth.adapter.out.security.Rs256JwtSupport;
import com.shinhan.pg.auth.config.AuthTokenProperties;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * JWKS 엔드포인트 계약 검증: kid가 AuthTokenProperties.keyId와 일치,
 * n/e가 Rs256JwtSupport 공개키와 일치, 개인키 파라미터 미포함, 발급 토큰의 kid와 일치.
 */
class JwksControllerTest {
    private final AuthTokenProperties properties = AuthTokenProperties.defaults();
    private final Rs256JwtSupport jwt = Rs256JwtSupport.generateEphemeral(properties.keyId());
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new JwksController(new JwksAdapter(jwt)))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode singleKey() throws Exception {
        var body = mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=300, public"))
                .andExpect(jsonPath("$.keys.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("keys").get(0);
    }

    @Test
    void kidMatchesConfiguredKeyId() throws Exception {
        assertEquals(properties.keyId(), singleKey().get("kid").asText());
    }

    @Test
    void modulusAndExponentMatchPublicKey() throws Exception {
        var jwk = singleKey();
        assertEquals("RSA", jwk.get("kty").asText());
        assertEquals("sig", jwk.get("use").asText());
        assertEquals("RS256", jwk.get("alg").asText());
        var modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("n").asText()));
        var exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("e").asText()));
        assertEquals(jwt.publicKey().getModulus(), modulus);
        assertEquals(jwt.publicKey().getPublicExponent(), exponent);
    }

    @Test
    void privateKeyMaterialIsNeverExposed() throws Exception {
        var jwk = singleKey();
        for (var privateField : new String[] {"d", "p", "q", "dp", "dq", "qi", "oth"}) {
            assertFalse(jwk.has(privateField), "JWKS에 개인키 파라미터 " + privateField + " 포함");
        }
    }

    @Test
    void jwksKidMatchesIssuedTokenHeaderKid() throws Exception {
        var token = jwt.issue(UUID.randomUUID(), properties.issuer(), properties.audience(),
                new Rs256JwtSupport.DurationSpec(Duration.ofMinutes(15)));
        var headerJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
        var tokenKid = mapper.readTree(headerJson).get("kid").asText();
        assertEquals(tokenKid, singleKey().get("kid").asText());
    }
}

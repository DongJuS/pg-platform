package com.shinhan.pg.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shinhan.pg.auth.domain.security.TotpMock;
import com.shinhan.pg.common.archrules.InfrastructureTestBase;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * [MVP1-F01..F06] 실제 PostgreSQL·Redis 위의 인수 테스트. Flyway 마이그레이션과 JDBC 계약을 함께 검증한다.
 * 신원 attribute(JWT→actorUserId 연결)는 gateway JWT 필터 구현 전까지 request attribute 계약을 그대로 쓴다.
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@SpringBootTest(properties = "spring.profiles.active=skeleton")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApiIntegrationTest extends InfrastructureTestBase {
    private static final byte[] OTP_SECRET =
            HexFormat.of().parseHex("00112233445566778899aabbccddeeff");
    private static final Pattern ACCESS = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REFRESH = Pattern.compile("\"refreshToken\"\\s*:\\s*\"([^\"]+)\"");

    @DynamicPropertySource
    static void authProperties(DynamicPropertyRegistry registry) {
        registry.add("pg.auth.otp-server-secret-hex",
                () -> HexFormat.of().formatHex(OTP_SECRET));
    }

    @Autowired MockMvc mvc;

    @Test
    @Order(1)
    void registerRejectsDuplicateEmail() throws Exception {
        var email = "reg-" + UUID.randomUUID() + "@example.invalid";
        register(email);
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"stronger1pass\","
                                + " \"name\": \"통합테스트\", \"phoneNumber\": \"010-0000-0000\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(2)
    void loginRefreshMeAndLogoutFlow() throws Exception {
        var user = register("flow-" + UUID.randomUUID() + "@example.invalid");
        var login = login(user.email());
        var loginBody = login.getResponse().getContentAsString();
        var accessToken = extract(ACCESS, loginBody);
        var refreshToken = extract(REFRESH, loginBody);

        var refreshed = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        var rotatedRefresh = extract(REFRESH, refreshed.getResponse().getContentAsString());

        mvc.perform(get("/api/v1/users/me")
                        .requestAttr("actorUserId", user.userId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.userId().toString()))
                .andExpect(jsonPath("$.name").value("통합테스트"));

        mvc.perform(post("/api/v1/auth/logout")
                        .requestAttr("actorUserId", user.userId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + rotatedRefresh + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void refreshTokenReuseIsRejected() throws Exception {
        var user = register("reuse-" + UUID.randomUUID() + "@example.invalid");
        var login = login(user.email());
        var refreshToken = extract(REFRESH, login.getResponse().getContentAsString());
        var rotated = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var rotatedToken = extract(REFRESH, rotated.getResponse().getContentAsString());

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + rotatedToken + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    void verifyOtpAcceptsCurrentTotpCodeAndRejectsReplay() throws Exception {
        var otpUser = register("otp-" + UUID.randomUUID() + "@example.invalid");
        var code = TotpMock.codeAt(TotpMock.seedFor(OTP_SECRET, otpUser.userId()), Instant.now());
        var draftId = UUID.randomUUID();
        mvc.perform(post("/api/v1/auth/otp/verify")
                        .requestAttr("actorUserId", otpUser.userId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferDraftId\": \"" + draftId + "\","
                                + " \"otpCode\": \"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpToken").isString());

        mvc.perform(post("/api/v1/auth/otp/verify")
                        .requestAttr("actorUserId", otpUser.userId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferDraftId\": \"" + draftId + "\","
                                + " \"otpCode\": \"" + code + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    /** 회원가입하고 userId를 attribute로 쓸 수 있도록 (이메일, userId)를 반환한다. */
    private RegisteredUser register(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"stronger1pass\","
                                + " \"name\": \"통합테스트\", \"phoneNumber\": \"010-0000-0000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isString())
                .andReturn();
        var userId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.userId"));
        return new RegisteredUser(email, userId);
    }

    private record RegisteredUser(String email, UUID userId) {}

    private MvcResult login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\","
                                + " \"password\": \"stronger1pass\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private static String extract(Pattern pattern, String body) {
        var matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("token missing in " + body);
        }
        return matcher.group(1);
    }
}

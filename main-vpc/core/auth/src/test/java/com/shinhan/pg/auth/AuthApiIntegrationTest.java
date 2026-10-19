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
@SpringBootTest(properties = {"spring.profiles.active=skeleton", "pg.internal.service-token=test-internal-secret"})
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
                        .header("X-User-Id", user.userId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.userId().toString()))
                .andExpect(jsonPath("$.name").value("통합테스트"));

        mvc.perform(post("/api/v1/auth/logout")
                        .header("X-User-Id", user.userId().toString())
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
                        .header("X-User-Id", otpUser.userId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferDraftId\": \"" + draftId + "\","
                                + " \"otpCode\": \"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpToken").isString());

        mvc.perform(post("/api/v1/auth/otp/verify")
                        .header("X-User-Id", otpUser.userId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transferDraftId\": \"" + draftId + "\","
                                + " \"otpCode\": \"" + code + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @Order(5)
    void lockedOtpReturns423RatherThanGeneric403() throws Exception {
        var user = register("locked-" + UUID.randomUUID() + "@example.invalid");
        var draft = UUID.randomUUID();
        for (int i=0; i<5; i++) {
            mvc.perform(post("/api/v1/auth/otp/verify").header("X-User-Id", user.userId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transferDraftId\":\""+draft+"\",\"otpCode\":\"invalid\"}"))
                .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/otp/verify").header("X-User-Id", user.userId().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"transferDraftId\":\""+draft+"\",\"otpCode\":\"invalid\"}"))
            .andExpect(status().isLocked());
    }

    @Test @Order(6)
    void otpAuthorizationIsBoundToUserAndOneLogicalTransfer() throws Exception {
        var user = register("consume-" + UUID.randomUUID() + "@example.invalid");
        var token = issueOtp(user.userId());
        consumeOtp(user.userId(), token, "one-request", 1000).andExpect(status().isNoContent());
        consumeOtp(user.userId(), token, "one-request", 1000).andExpect(status().isNoContent());
        consumeOtp(user.userId(), token, "another-request", 1000).andExpect(status().isUnauthorized());
        consumeOtp(user.userId(), token, "one-request", 2000).andExpect(status().isUnauthorized());
        consumeOtp(UUID.randomUUID(), token, "one-request", 1000).andExpect(status().isUnauthorized());
        consumeOtp(user.userId(), "forged", "new-request", 1000).andExpect(status().isUnauthorized());
    }

    @Test @Order(7)
    void concurrentDifferentTransfersCannotShareOneOtpAuthorization() throws Exception {
        var user = register("race-" + UUID.randomUUID() + "@example.invalid");
        var token = issueOtp(user.userId());
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> { ready.countDown(); start.await(); return consumeOtp(user.userId(), token, "race-a", 1000).andReturn().getResponse().getStatus(); });
            var second = workers.submit(() -> { ready.countDown(); start.await(); return consumeOtp(user.userId(), token, "race-b", 1000).andReturn().getResponse().getStatus(); });
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            var results = new java.util.ArrayList<>(java.util.List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS), second.get(10, java.util.concurrent.TimeUnit.SECONDS)));
            results.sort(Integer::compareTo);
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(204, 401), results);
        }
    }

    private String issueOtp(UUID user) throws Exception {
        var code = TotpMock.codeAt(TotpMock.seedFor(OTP_SECRET, user), Instant.now());
        var result = mvc.perform(post("/api/v1/auth/otp/verify").header("X-User-Id", user.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"transferDraftId\":\""+UUID.randomUUID()+"\",\"otpCode\":\""+code+"\"}"))
            .andExpect(status().isOk()).andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString()).get("otpToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions consumeOtp(UUID user, String token, String key, int amount) throws Exception {
        var body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of(
            "actorUserId", user.toString(), "otpToken", token, "idempotencyKey", key,
            "fromAccountId", "00000000-0000-0000-0000-000000000001",
            "toAccountId", "00000000-0000-0000-0000-000000000002", "amount", amount, "currency", "KRW"));
        return mvc.perform(post("/internal/v1/auth/otp/consume")
            .header("X-Internal-Caller", "transfer").header("X-Internal-Service-Token", "test-internal-secret")
            .contentType(MediaType.APPLICATION_JSON).content(body));
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

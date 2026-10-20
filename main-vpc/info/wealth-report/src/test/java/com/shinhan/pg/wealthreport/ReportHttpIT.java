package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.wealthreport.application.command.GenerateReportCommand;
import com.shinhan.pg.wealthreport.application.port.out.ReportSnapshotPort;
import com.shinhan.pg.wealthreport.domain.model.AccountFigures;
import com.shinhan.pg.wealthreport.domain.model.AssetSnapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
@EnabledIfSystemProperty(named = "integration", matches = "true")
@SpringBootTest(
        properties = {
            "platform.report.policy.confirmed=true",
            "management.tracing.enabled=false",
            "management.health.kafka.enabled=false",
            "management.health.redis.enabled=false"
        })
@ActiveProfiles({"skeleton", "report-test"})
@AutoConfigureMockMvc
class ReportHttpIT {
    @Container
    static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl);
        r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean ReportSnapshotPort snapshots;
    UUID user = UUID.randomUUID(), account = UUID.randomUUID();
    String body;

    @BeforeEach
    void setup() throws Exception {
        jdbc.execute(
                "truncate"
                    + " asset_reports,report_stages,asset_snapshots,snapshot_accounts,report_files"
                    + " cascade");
        var asOf = ReportPoliciesTest.AS_OF;
        body =
                json.writeValueAsString(
                        Map.of(
                                "from",
                                asOf.minusSeconds(86400),
                                "to",
                                asOf,
                                "asOf",
                                asOf,
                                "accountIds",
                                List.of(account)));
        when(snapshots.load(any()))
                .thenAnswer(
                        call -> {
                            GenerateReportCommand c = call.getArgument(0);
                            return new AssetSnapshot(
                                    UUID.randomUUID(),
                                    c.actorUserId(),
                                    c.from(),
                                    c.to(),
                                    c.asOf(),
                                    asOf,
                                    "test-read",
                                    List.of(
                                            new AccountFigures(
                                                    account,
                                                    new BigDecimal("90000"),
                                                    new BigDecimal("10000"),
                                                    new BigDecimal("5000"),
                                                    "KRW",
                                                    1)));
                        });
    }

    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req(String b) {
        return post("/api/v1/wealth/reports")
                .header("X-User-Id", user)
                .header("Idempotency-Key", "http-test")
                .contentType("application/json")
                .content(b);
    }

    @Test
    void actualHttpGenerationAndPrivatePdfOwnership() throws Exception {
        var response =
                mvc.perform(req(body))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.draftSource").value("TEST_DOUBLE"))
                        .andReturn()
                        .getResponse();
        var tree = json.readTree(response.getContentAsString());
        String id = tree.get("reportId").asText(), url = tree.get("downloadUrl").asText();
        byte[] pdf =
                mvc.perform(get(url).header("X-User-Id", user))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType("application/pdf"))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        assertTrue(pdf.length > 1000);
        for (String suffix : List.of("", "/pdf"))
            mvc.perform(
                            get("/api/v1/wealth/reports/" + id + suffix)
                                    .header("X-User-Id", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(req(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value(id));
        verify(snapshots, times(1)).load(any());
    }

    @Test
    void identityAndPayloadValidationPrecedeAnySourceCall() throws Exception {
        mvc.perform(
                        post("/api/v1/wealth/reports")
                                .header("Idempotency-Key", "k")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(req(body).header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
        mvc.perform(req(body.substring(0, body.length() - 1) + ",\"actorUserId\":\"secret\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        req(
                                body.substring(0, body.length() - 1)
                                        + ",\"asOf\":\"2026-10-19T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(req(body + " {} ")).andExpect(status().isBadRequest());
        mvc.perform(req("a".repeat(20000))).andExpect(status().isPayloadTooLarge());
        verifyNoInteractions(snapshots);
    }

    @Test
    void filtersAndMissingFieldsHaveSafeErrors() throws Exception {
        mvc.perform(req("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/wealth/reports").header("X-User-Id", user).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/api/v1/wealth/reports")
                                .header("X-User-Id", user)
                                .param("status", "arbitrary-secret"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/wealth/reports").header("X-User-Id", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reports").isEmpty());
    }
}

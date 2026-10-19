package com.shinhan.pg.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.shinhan.pg.account.adapter.in.web.AccountController;
import com.shinhan.pg.account.adapter.in.web.PlatformExceptionHandler;
import com.shinhan.pg.account.adapter.out.persistence.AccountPersistenceAdapter;
import com.shinhan.pg.account.adapter.out.persistence.AccountWriteAdapter;
import com.shinhan.pg.account.adapter.out.ledger.LedgerAccountRestSkeleton;
import com.shinhan.pg.account.application.service.AccountApplicationService;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

/**
 * [MVP1-F07..F10] 4개 엔드포인트가 실제 MariaDB 위에서 동작하는지 HTTP 레벨로 검증한다.
 * 신원은 §3 계약의 X-User-Id 헤더, F10은 X-Internal-Caller 헤더를 사용한다.
 * (gateway JWT 연결·스택 레벨 e2e는 WO-A2/WO-B1 범위.)
 */
@EnabledIfSystemProperty(named = "integration", matches = "true")
@Testcontainers
class AccountApiIT {
    private static final UUID OWNER =
            UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID OTHER =
            UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> MARIADB =
            new GenericContainer<>(DockerImageName.parse("mariadb:11"))
                    .withEnv("MARIADB_ROOT_PASSWORD", "test")
                    .withEnv("MARIADB_DATABASE", "account_db")
                    .withExposedPorts(3306)
                    .waitingFor(Wait.forListeningPort());

    private MockMvc mvc;

    @BeforeEach
    void migrateAndWire() {
        var url =
                "jdbc:mariadb://"
                        + MARIADB.getHost()
                        + ":"
                        + MARIADB.getMappedPort(3306)
                        + "/account_db";
        var source = new DriverManagerDataSource(url, "root", "test");
        Flyway.configure()
                .dataSource(source)
                .locations("classpath:db/migration")
                .load()
                .migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("delete from accounts");
        var service =
                new AccountApplicationService(
                        new LedgerAccountRestSkeleton(),
                        new AccountWriteAdapter(jdbc),
                        new AccountPersistenceAdapter(jdbc));
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new AccountController(service, service, service, service))
                        .setControllerAdvice(new PlatformExceptionHandler())
                        .build();
    }

    @Test
    void openListAndGetFlowWorksOnRealDatabase() throws Exception {
        // F07: 개설 201, 상태 ACTIVE, 서버 생성 계좌번호
        var opened =
                mvc.perform(
                                post("/api/v1/accounts")
                                        .header("X-User-Id", OWNER.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"currency\": \"KRW\", \"alias\": \"입출금\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.accountId").isString())
                        .andExpect(jsonPath("$.accountNumber").isString())
                        .andExpect(jsonPath("$.status").value("ACTIVE"))
                        .andReturn();
        var body = opened.getResponse().getContentAsString();
        var accountId =
                body.replaceAll(".*\"accountId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // F08: 본인 목록 200
        mvc.perform(get("/api/v1/accounts").header("X-User-Id", OWNER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts.length()").value(1))
                .andExpect(jsonPath("$.accounts[0].alias").value("입출금"));

        // F09: 소유자 상세 200 / 타인 403 / 없는 계좌 404
        mvc.perform(
                        get("/api/v1/accounts/{id}", accountId)
                                .header("X-User-Id", OWNER.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(OWNER.toString()));
        mvc.perform(
                        get("/api/v1/accounts/{id}", accountId)
                                .header("X-User-Id", OTHER.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT-4031"));
        mvc.perform(
                        get("/api/v1/accounts/{id}", UUID.randomUUID().toString())
                                .header("X-User-Id", OWNER.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT-4040"));
    }

    @Test
    void publicEndpointsRejectMissingUserIdWith401() throws Exception {
        mvc.perform(
                        post("/api/v1/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currency\": \"KRW\", \"alias\": \"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT-4010"));
        mvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void openAccountRejectsNonKrwWith400() throws Exception {
        mvc.perform(
                        post("/api/v1/accounts")
                                .header("X-User-Id", OWNER.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"currency\": \"USD\", \"alias\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACCOUNT-4000"));
    }

    @Test
    void statusChangeRequiresInternalCallerAndAllowsOnlyActiveSuspended() throws Exception {
        var opened =
                mvc.perform(
                                post("/api/v1/accounts")
                                        .header("X-User-Id", OWNER.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"currency\": \"KRW\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        var accountId =
                opened.getResponse()
                        .getContentAsString()
                        .replaceAll(".*\"accountId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        // X-Internal-Caller 없으면 403
        mvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"SUSPENDED\", \"reason\": \"점검\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT-4030"));

        // 내부 호출자 표식이 있으면 ACTIVE→SUSPENDED 200
        mvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .header("X-Internal-Caller", "transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"SUSPENDED\", \"reason\": \"점검\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // 허용되지 않는 전이(SUSPENDED→CLOSED)는 409
        mvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .header("X-Internal-Caller", "transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"CLOSED\", \"reason\": \"해지\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT-4090"));

        // SUSPENDED→ACTIVE 복구 200
        mvc.perform(
                        patch("/api/v1/accounts/{id}/status", accountId)
                                .header("X-Internal-Caller", "transfer")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\": \"ACTIVE\", \"reason\": \"복구\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}

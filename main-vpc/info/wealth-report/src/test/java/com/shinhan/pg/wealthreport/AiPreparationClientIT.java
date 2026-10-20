package com.shinhan.pg.wealthreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shinhan.pg.wealthreport.adapter.out.aigateway.AiPreparationRestAdapter;
import com.shinhan.pg.wealthreport.domain.error.ReportException;
import com.shinhan.pg.wealthreport.domain.error.WealthReportErrorCode;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@EnabledIfSystemProperty(named = "integration", matches = "true")
class AiPreparationClientIT {
    HttpServer server;
    AtomicInteger calls;
    int status;
    String response;
    String receivedCaller, receivedToken, receivedBody, receivedPath;

    @BeforeEach
    void start() throws Exception {
        calls = new AtomicInteger();
        status = 200;
        response = ready(false);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/",
                exchange -> {
                    calls.incrementAndGet();
                    receivedPath = exchange.getRequestURI().getPath();
                    receivedCaller = exchange.getRequestHeaders().getFirst("X-Internal-Caller");
                    receivedToken =
                            exchange.getRequestHeaders().getFirst("X-Internal-Service-Token");
                    receivedBody =
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8);
                    byte[] body = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(status, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    String ready(boolean external) {
        return "{\"requestId\":\""
                + UUID.randomUUID()
                + "\",\"status\":\"READY_FOR_TRANSFER\",\"reason\":\"CHECKS_PASSED\",\"maskedPrompt\":\"지표"
                + " 설명\",\"maskedCounts\":{},\"policyVersion\":\"preparation-rules-v1\",\"externalTransferPerformed\":"
                + external
                + "}";
    }

    AiPreparationRestAdapter client(String token) {
        return new AiPreparationRestAdapter(
                new MockEnvironment()
                        .withProperty(
                                "pg.clients.ai-gateway.base-url",
                                "http://127.0.0.1:" + server.getAddress().getPort())
                        .withProperty("pg.clients.ai-gateway.preparation-token", token)
                        .withProperty("pg.internal.service-token", "must-not-be-used"));
    }

    @Test
    void usesCallerCredentialAndOnlyTheActivePreparationEndpoint() {
        client("synthetic-preparation-credential").checkAggregateText("지표 설명");
        assertEquals(1, calls.get());
        assertEquals("wealth-report", receivedCaller);
        assertEquals("synthetic-preparation-credential", receivedToken);
        assertEquals("/internal/v1/ai/preparations", receivedPath);
        assertTrue(receivedBody.contains("sensitiveValues"));
        assertFalse(receivedBody.contains("must-not-be-used"));
    }

    @Test
    void blankCredentialDoesNotFallBackToFinanceToken() {
        assertEquals(
                WealthReportErrorCode.AI_NOT_CONFIGURED,
                assertThrows(ReportException.class, () -> client("").checkAggregateText("지표 설명"))
                        .code());
        assertEquals(0, calls.get());
    }

    @Test
    void transportErrorsDoNotEchoCredentialsOrBodyOrRetry() {
        status = 403;
        response = "private-provider-fragment";
        var error =
                assertThrows(
                        ReportException.class,
                        () -> client("secret-preparation").checkAggregateText("private-prompt"));
        assertFalse(error.toString().contains("private"));
        assertNull(error.getCause());
        assertEquals(1, calls.get());
    }

    @Test
    void malformedBlockedAndTransferredResultsAreNeverReady() {
        for (String invalid : new String[] {"{}", "{\"status\":\"BLOCKED\"}", ready(true)}) {
            response = invalid;
            assertThrows(ReportException.class, () -> client("token").checkAggregateText("지표 설명"));
        }
    }
}

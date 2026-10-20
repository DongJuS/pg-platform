package com.shinhan.pg.transfer.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalServiceAuthenticationFilterTest {
    @Test
    void encodedInternalPathCannotBypassServiceAuthentication() throws Exception {
        var request = new MockHttpServletRequest("GET", "/%69nternal/v1/probe");
        request.setServletPath("/internal/v1/probe");
        var response = new MockHttpServletResponse();
        var filter =
                new InternalServiceAuthenticationFilter(
                        new MockEnvironment()
                                .withProperty("pg.internal.service-token", "test-secret"),
                        new ObjectMapper());
        filter.doFilter(
                request,
                response,
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        assertEquals(403, response.getStatus());
    }

    @Test
    void applicationContextPathCannotHideAnInternalEndpoint() throws Exception {
        var request = new MockHttpServletRequest("GET", "/bank/internal/v1/probe");
        request.setContextPath("/bank");
        request.setServletPath("/internal/v1/probe");
        var response = new MockHttpServletResponse();
        var filter =
                new InternalServiceAuthenticationFilter(
                        new MockEnvironment()
                                .withProperty("pg.internal.service-token", "test-secret"),
                        new ObjectMapper());
        filter.doFilter(
                request,
                response,
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        assertEquals(403, response.getStatus());
    }

    @Test
    void authenticatedInternalRequestStillReachesItsHandler() throws Exception {
        var request = new MockHttpServletRequest("GET", "/internal/v1/probe");
        request.addHeader("X-Internal-Caller", "test-caller");
        request.addHeader("X-Internal-Service-Token", "test-secret");
        var response = new MockHttpServletResponse();
        var filter =
                new InternalServiceAuthenticationFilter(
                        new MockEnvironment()
                                .withProperty("pg.internal.service-token", "test-secret"),
                        new ObjectMapper());
        filter.doFilter(
                request,
                response,
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        assertEquals(204, response.getStatus());
    }

    @Test
    void swiftAdministratorRequiresItsOwnCredentialAndRejectsAmbiguousHeaders() throws Exception {
        var env =
                new MockEnvironment()
                        .withProperty("pg.swift.enabled", "true")
                        .withProperty("pg.internal.service-token", "financial")
                        .withProperty("pg.swift.caller-tokens.swift-operations", "operations");
        for (String supplied : java.util.List.of("financial", "operations")) {
            var req =
                    new MockHttpServletRequest(
                            "POST", "/internal/v1/swift/transfers/id/retransmissions");
            req.addHeader("X-Internal-Caller", "swift-operations");
            req.addHeader("X-Internal-Service-Token", supplied);
            var res = new MockHttpServletResponse();
            new InternalServiceAuthenticationFilter(env, new ObjectMapper())
                    .doFilter(
                            req,
                            res,
                            (a, b) ->
                                    ((jakarta.servlet.http.HttpServletResponse) b).setStatus(204));
            assertEquals("operations".equals(supplied) ? 204 : 403, res.getStatus());
        }
        var req = new MockHttpServletRequest("GET", "/internal/v1/swift/transfers/id/messages");
        req.addHeader("X-Internal-Caller", "swift-operations");
        req.addHeader("X-Internal-Caller", "transfer");
        req.addHeader("X-Internal-Service-Token", "operations");
        var res = new MockHttpServletResponse();
        new InternalServiceAuthenticationFilter(env, new ObjectMapper())
                .doFilter(
                        req,
                        res,
                        (a, b) -> ((jakarta.servlet.http.HttpServletResponse) b).setStatus(204));
        assertEquals(403, res.getStatus());
    }

    @Test
    void swiftScopedAuthenticationDoesNotChangePgFinancialCredentials() throws Exception {
        var env =
                new MockEnvironment()
                        .withProperty("pg.swift.enabled", "true")
                        .withProperty("pg.internal.service-token", "financial");
        var req = new MockHttpServletRequest("POST", "/internal/v1/fep/pg/operations");
        req.addHeader("X-Internal-Caller", "payment");
        req.addHeader("X-Internal-Service-Token", "financial");
        var res = new MockHttpServletResponse();
        new InternalServiceAuthenticationFilter(env, new ObjectMapper())
                .doFilter(
                        req,
                        res,
                        (a, b) -> ((jakarta.servlet.http.HttpServletResponse) b).setStatus(204));
        assertEquals(204, res.getStatus());
    }
}

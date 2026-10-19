package com.shinhan.pg.transfer.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalServiceAuthenticationFilterTest {
    @Test void encodedInternalPathCannotBypassServiceAuthentication() throws Exception {
        var request = new MockHttpServletRequest("GET", "/%69nternal/v1/probe");
        request.setServletPath("/internal/v1/probe");
        var response = new MockHttpServletResponse();
        var filter = new InternalServiceAuthenticationFilter(
            new MockEnvironment().withProperty("pg.internal.service-token", "test-secret"), new ObjectMapper());
        filter.doFilter(request, response, (req,res) -> ((jakarta.servlet.http.HttpServletResponse)res).setStatus(204));
        assertEquals(403, response.getStatus());
    }
    @Test void applicationContextPathCannotHideAnInternalEndpoint() throws Exception {
        var request = new MockHttpServletRequest("GET", "/bank/internal/v1/probe");
        request.setContextPath("/bank"); request.setServletPath("/internal/v1/probe");
        var response = new MockHttpServletResponse();
        var filter = new InternalServiceAuthenticationFilter(
            new MockEnvironment().withProperty("pg.internal.service-token", "test-secret"), new ObjectMapper());
        filter.doFilter(request, response, (req,res) -> ((jakarta.servlet.http.HttpServletResponse)res).setStatus(204));
        assertEquals(403, response.getStatus());
    }
    @Test void authenticatedInternalRequestStillReachesItsHandler() throws Exception {
        var request = new MockHttpServletRequest("GET", "/internal/v1/probe");
        request.addHeader("X-Internal-Caller", "test-caller");
        request.addHeader("X-Internal-Service-Token", "test-secret");
        var response = new MockHttpServletResponse();
        var filter = new InternalServiceAuthenticationFilter(
            new MockEnvironment().withProperty("pg.internal.service-token", "test-secret"), new ObjectMapper());
        filter.doFilter(request, response, (req,res) -> ((jakarta.servlet.http.HttpServletResponse)res).setStatus(204));
        assertEquals(204, response.getStatus());
    }
}

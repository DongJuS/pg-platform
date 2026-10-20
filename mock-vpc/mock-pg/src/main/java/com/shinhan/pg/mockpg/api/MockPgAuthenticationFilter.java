package com.shinhan.pg.mockpg.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public final class MockPgAuthenticationFilter extends OncePerRequestFilter {
    private final String token;

    public MockPgAuthenticationFilter(Environment env) {
        token = env.getProperty("pg.provider.token", "");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest r, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path =
                org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(
                        r);
        if (path.startsWith("/mock/")) {
            String supplied = r.getHeader("X-Mock-PG-Token");
            if (token.isBlank()
                    || supplied == null
                    || !MessageDigest.isEqual(
                            token.getBytes(StandardCharsets.UTF_8),
                            supplied.getBytes(StandardCharsets.UTF_8))) {
                response.setStatus(token.isBlank() ? 503 : 403);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"MOCK-PG-AUTH\"}");
                return;
            }
        }
        chain.doFilter(r, response);
    }
}

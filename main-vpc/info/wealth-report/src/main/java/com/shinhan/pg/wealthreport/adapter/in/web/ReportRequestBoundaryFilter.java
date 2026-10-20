package com.shinhan.pg.wealthreport.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

/**
 * Bound actual/chunked report input and reject ambiguous trusted identity headers before parsing.
 */
@Component
@Profile("skeleton")
@Order(10)
public final class ReportRequestBoundaryFilter extends OncePerRequestFilter {
    protected void doFilterInternal(
            HttpServletRequest r, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!r.getRequestURI().startsWith("/api/v1/wealth/reports")) {
            chain.doFilter(r, response);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        if (Collections.list(r.getHeaders("X-User-Id")).size() > 1) {
            reject(response, 400);
            return;
        }
        if (!"POST".equals(r.getMethod())) {
            chain.doFilter(r, response);
            return;
        }
        if (Collections.list(r.getHeaders("Idempotency-Key")).size() > 1) {
            reject(response, 400);
            return;
        }
        byte[] data = r.getInputStream().readNBytes(16385);
        if (data.length > 16384) {
            reject(response, 413);
            return;
        }
        chain.doFilter(
                new HttpServletRequestWrapper(r) {
                    public ServletInputStream getInputStream() {
                        var input = new ByteArrayInputStream(data);
                        return new ServletInputStream() {
                            public int read() {
                                return input.read();
                            }

                            public int read(byte[] b, int off, int len) {
                                return input.read(b, off, len);
                            }

                            public boolean isFinished() {
                                return input.available() == 0;
                            }

                            public boolean isReady() {
                                return true;
                            }

                            public void setReadListener(ReadListener listener) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }

                    public BufferedReader getReader() {
                        return new BufferedReader(
                                new InputStreamReader(
                                        getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                },
                response);
    }

    private void reject(HttpServletResponse r, int status) throws IOException {
        r.setStatus(status);
        r.setContentType("application/json");
        r.getWriter()
                .write(
                        "{\"code\":\"REPORT-INVALID_INPUT\",\"message\":\"Report request"
                                + " rejected\"}");
    }
}

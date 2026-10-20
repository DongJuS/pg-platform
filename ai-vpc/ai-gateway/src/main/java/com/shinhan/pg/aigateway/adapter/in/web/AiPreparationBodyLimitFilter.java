package com.shinhan.pg.aigateway.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Bounds the actual stream (including chunked requests), after authentication and before Jackson. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 7)
public final class AiPreparationBodyLimitFilter extends OncePerRequestFilter {
    public static final int MAX_BODY_BYTES = 65_536;
    private final ObjectMapper json;
    public AiPreparationBodyLimitFilter(ObjectMapper json) { this.json = json; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (!"/internal/v1/ai/preparations".equals(path) || !"POST".equals(request.getMethod())) {
            chain.doFilter(request, response); return;
        }
        response.setHeader("Cache-Control", "no-store");
        if (request.getContentLengthLong() > MAX_BODY_BYTES) { reject(response, 413, "INPUT_TOO_LARGE"); return; }
        if (request.getCharacterEncoding() != null && !"UTF-8".equalsIgnoreCase(request.getCharacterEncoding())) {
            reject(response, 415, "UNSUPPORTED_FORMAT"); return;
        }
        byte[] body;
        try { body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1); }
        catch (IOException inputFailure) { reject(response, 400, "INVALID_INPUT"); return; }
        if (body.length > MAX_BODY_BYTES) { reject(response, 413, "INPUT_TOO_LARGE"); return; }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var stream = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override public int read() { return stream.read(); }
                    @Override public int read(byte[] b, int off, int length) { return stream.read(b, off, length); }
                    @Override public boolean isFinished() { return stream.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException("Synchronous input only");
                    }
                };
            }
            @Override public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
            }
        }, response);
    }
    private void reject(HttpServletResponse response, int status, String reason) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        json.writeValue(response.getWriter(), new ErrorResponse("AI-" + reason, reason, MDC.get("traceId")));
    }
}

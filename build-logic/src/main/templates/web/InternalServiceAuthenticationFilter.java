package @@WEB_PACKAGE@@;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Internal API authentication. Missing configuration fails closed; no development secret fallback. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 8)
public final class InternalServiceAuthenticationFilter extends OncePerRequestFilter {
    private final Environment environment;
    private final ObjectMapper json;
    public InternalServiceAuthenticationFilter(Environment environment, ObjectMapper json) {
        this.environment = environment; this.json = json;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        boolean legacyStatus = "PATCH".equals(request.getMethod()) && path.matches("/api/v1/accounts/[^/]+/status");
        if (!path.startsWith("/internal/") && !legacyStatus) { chain.doFilter(request, response); return; }
        String supplied = request.getHeader("X-Internal-Service-Token");
        String caller = request.getHeader("X-Internal-Caller");
        boolean swiftPath = environment.getProperty("pg.swift.enabled", Boolean.class, false)
                && (path.startsWith("/internal/v1/swift/") || path.startsWith("/internal/v1/fep/swift/")
                    || path.startsWith("/internal/v1/transfers/overseas/") || path.equals("/internal/v1/ledger/overseas-reservations"));
        boolean boundCaller = swiftPath || environment.getProperty("pg.internal.require-caller-token", Boolean.class, false);
        boolean validCaller = caller != null && caller.matches("[a-z][a-z0-9-]{0,47}");
        // Opt-in for AI preparation: a caller header is authenticated only with its own credential.
        // Existing financial services retain their current shared-token contract.
        String expected = boundCaller
                ? (validCaller ? environment.getProperty((swiftPath ? "pg.swift.caller-tokens." : "pg.internal.caller-tokens.") + caller, "") : "")
                : environment.getProperty("pg.internal.service-token", "");
        boolean ambiguous = boundCaller && (java.util.Collections.list(request.getHeaders("X-Internal-Caller")).size() != 1
                || java.util.Collections.list(request.getHeaders("X-Internal-Service-Token")).size() != 1);
        if (ambiguous || expected.isBlank() || supplied == null || caller == null || caller.isBlank()
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(expected.isBlank() && !boundCaller ? 503 : 403);
            response.setContentType("application/json");
            json.writeValue(response.getWriter(), new ErrorResponse("COMMON-INTERNAL-AUTH", "Internal service authentication required", MDC.get("traceId")));
            return;
        }
        request.setAttribute("callerServiceId", caller);
        chain.doFilter(request, response);
    }
}

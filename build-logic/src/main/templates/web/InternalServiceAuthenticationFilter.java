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
        String expected = environment.getProperty("pg.internal.service-token", "");
        String supplied = request.getHeader("X-Internal-Service-Token");
        String caller = request.getHeader("X-Internal-Caller");
        if (expected.isBlank() || supplied == null || caller == null || caller.isBlank()
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(expected.isBlank() ? 503 : 403);
            response.setContentType("application/json");
            json.writeValue(response.getWriter(), new ErrorResponse("COMMON-INTERNAL-AUTH", "Internal service authentication required", MDC.get("traceId")));
            return;
        }
        request.setAttribute("callerServiceId", caller);
        chain.doFilter(request, response);
    }
}

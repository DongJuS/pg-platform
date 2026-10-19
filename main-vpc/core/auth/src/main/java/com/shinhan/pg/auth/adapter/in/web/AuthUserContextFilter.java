package com.shinhan.pg.auth.adapter.in.web;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;
/** Gateway verified JWT subject -> existing Auth controller context. */
@Component @Profile("skeleton") @Order(Ordered.LOWEST_PRECEDENCE - 9)
public final class AuthUserContextFilter extends OncePerRequestFilter {
    private final ObjectMapper json;
    public AuthUserContextFilter(ObjectMapper json) { this.json = json; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (path.equals("/api/v1/users/me") || path.equals("/api/v1/auth/otp/verify")) {
            UUID actor;
            try { actor = UUID.fromString(request.getHeader("X-User-Id")); }
            catch (IllegalArgumentException | NullPointerException invalid) {
                response.setStatus(401); response.setContentType("application/json");
                json.writeValue(response.getWriter(), new ErrorResponse("AUTH-4010", "Authenticated user required", org.slf4j.MDC.get("traceId")));
                return;
            }
            request.setAttribute("actorUserId", actor);
        }
        chain.doFilter(request, response);
    }
}

package com.shinhan.pg.ledger.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shinhan.pg.common.commonerror.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * <pre>
 * 인증 문맥 계약(지시서 §3): gateway는 검증된 JWT의 sub(userId)를 X-User-Id 헤더로 downstream에
 * 추가하고, 외부에서 들어온 X-User-Id·X-Internal-Caller 헤더는 제거한다(스푸핑 차단).
 * 이 필터는 그 헤더를 Controller가 읽는 request attribute로 연결한다.
 *   - PUBLIC(/api/v1/**): X-User-Id → actorUserId(UUID). 없거나 UUID가 아니면 401.
 *   - INTERNAL(/internal/**): X-Internal-Caller → callerServiceId.
 *     TODO: 내부 호출의 서비스 인증 토큰 방식은 지시서 §5-③ 미정 — 헤더 부재를 거절하지 않는다.
 * </pre>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 9) // TraceFilter(LOWEST_PRECEDENCE-10) 뒤에 실행돼 MDC traceId를 쓴다.
public final class AuthContextFilter extends OncePerRequestFilter {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var path = org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        if (path != null && path.startsWith("/api/v1/")) {
            var header = request.getHeader("X-User-Id");
            UUID userId = null;
            if (header != null) {
                try {
                    userId = UUID.fromString(header);
                } catch (IllegalArgumentException ignored) {
                    // 아래 401 처리로 진행한다.
                }
            }
            if (userId == null) {
                rejectUnauthorized(response);
                return;
            }
            request.setAttribute("actorUserId", userId);
        }
        if (path != null && path.startsWith("/internal/")) {
            var caller = request.getHeader("X-Internal-Caller");
            if (caller != null && !caller.isBlank()) {
                request.setAttribute("callerServiceId", caller);
            }
        }
        chain.doFilter(request, response);
    }

    private void rejectUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        JSON.writeValue(
                response.getWriter(),
                new ErrorResponse(
                        "LEDGER-UNAUTHORIZED",
                        "Missing or invalid X-User-Id",
                        MDC.get("traceId")));
    }
}

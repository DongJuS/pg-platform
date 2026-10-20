package com.shinhan.pg.payment.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public final class PaymentIdentityFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path =
                org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(
                        request);
        if (path.startsWith("/api/v1/")) {
            try {
                request.setAttribute(
                        "actorUserId", UUID.fromString(request.getHeader("X-User-Id")));
            } catch (IllegalArgumentException | NullPointerException invalid) {
                response.setStatus(401);
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":\"PAYMENT-UNAUTHORIZED\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

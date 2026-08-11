package com.skateboard.uibackend.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Every request gets a correlation id: the frontend's {@code X-Correlation-Id}
 * if it sent one, otherwise a generated one. It's put in MDC for logging
 * ({@code %X{correlationId}} in application.yml's log pattern), echoed back on
 * the response, and read by {@link CorrelationIdExchangeFilter} to propagate
 * the same value to downstream service calls made while handling this
 * request.
 * <p>
 * Ordered ahead of Spring Security's filter chain (which Boot registers at
 * {@code HIGHEST_PRECEDENCE}) so the correlation id is already in MDC when
 * {@link RestAuthenticationEntryPoint} handles a rejected-before-reaching-a-
 * controller request (e.g. a missing/invalid token).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}

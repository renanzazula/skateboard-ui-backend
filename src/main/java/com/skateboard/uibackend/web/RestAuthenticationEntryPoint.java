package com.skateboard.uibackend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skateboard.uibackend.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Requests rejected by the resource server (missing/expired/invalid token)
 * never reach a controller, so {@link com.skateboard.uibackend.exception.GlobalExceptionHandler}
 * can't shape their response — this does the equivalent for the 401 case, so
 * every error the frontend sees has the same {@link ErrorResponse} shape.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of("UNAUTHENTICATED", "A valid bearer token is required", MDC.get(CorrelationIdFilter.MDC_KEY));
        objectMapper.writeValue(response.getWriter(), body);
    }
}

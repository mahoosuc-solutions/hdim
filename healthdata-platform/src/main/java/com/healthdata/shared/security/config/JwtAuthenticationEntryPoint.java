package com.healthdata.shared.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point - Handles authentication errors
 *
 * Called when an unauthenticated request tries to access a protected resource.
 * Returns a 401 Unauthorized response with detailed error information in JSON format.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle authentication failure
     *
     * Returns 401 Unauthorized with error details in JSON format.
     * Logs the authentication failure for audit/debugging purposes.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param authException Authentication exception that occurred
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        log.error("Responding with unauthorized error. Message - {}", authException.getMessage());
        log.debug("Request URI: {}, Method: {}", request.getRequestURI(), request.getMethod());

        // Set response status and content type
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Build error response
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", authException.getMessage());
        body.put("path", request.getServletPath());
        body.put("timestamp", System.currentTimeMillis());

        // Write response
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

package com.healthdata.shared.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Access Denied Handler - Handles authorization errors
 *
 * Called when an authenticated user tries to access a resource they don't have permission for.
 * Returns a 403 Forbidden response with detailed error information in JSON format.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle access denied (authorization failure)
     *
     * Returns 403 Forbidden with error details in JSON format.
     * Logs the authorization failure for audit/debugging purposes.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param accessDeniedException Access denied exception that occurred
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("Responding with access denied error. Message - {}", accessDeniedException.getMessage());
        log.debug("Request URI: {}, Method: {}, User: {}",
                request.getRequestURI(), request.getMethod(), request.getUserPrincipal());

        // Set response status and content type
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // Build error response
        final Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", "Forbidden");
        body.put("message", "You do not have permission to access this resource");
        body.put("detail", accessDeniedException.getMessage());
        body.put("path", request.getServletPath());
        body.put("timestamp", System.currentTimeMillis());

        // Write response
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

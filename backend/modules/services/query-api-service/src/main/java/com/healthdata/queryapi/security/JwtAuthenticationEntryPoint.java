package com.healthdata.queryapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for JWT authentication failures.
 *
 * This component handles the case where a request lacks valid authentication
 * (no JWT token, expired token, invalid token, etc.) and returns a structured
 * JSON error response instead of the default HTTP 401 error page.
 *
 * Common Scenarios:
 * 1. Missing Authorization header
 * 2. Invalid JWT token format (malformed)
 * 3. Expired JWT token
 * 4. Invalid JWT signature
 * 5. Missing required JWT claims
 * 6. Token from untrusted issuer
 *
 * Response Format:
 * {
 *   "error": "Unauthorized",
 *   "message": "Full error description",
 *   "timestamp": 1699564800000
 * }
 *
 * HTTP Status: 401 Unauthorized
 *
 * @author HDIM Security Team
 * @version 1.0
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles authentication failures by returning a JSON error response.
     *
     * Process:
     * 1. Log the authentication failure with error details
     * 2. Set HTTP status to 401 Unauthorized
     * 3. Set Content-Type to application/json
     * 4. Write structured error response body
     * 5. Add cache prevention headers
     *
     * Security Headers:
     * - Cache-Control: no-store, no-cache, must-revalidate
     * - Pragma: no-cache
     * - Expires: 0
     * - WWW-Authenticate: Bearer realm="hdim-api"
     *
     * @param request HTTP request that triggered authentication
     * @param response HTTP response to write error to
     * @param authException The authentication exception with error details
     * @throws IOException if response writing fails
     * @throws ServletException if servlet processing fails
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        log.warn("Authentication failed for request: {} {} - Reason: {}",
                request.getMethod(),
                request.getRequestURI(),
                authException.getMessage());

        // Set HTTP status
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Set content type
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Add cache prevention headers (important for security-sensitive responses)
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Add WWW-Authenticate header (RFC 6750 for Bearer token)
        response.setHeader("WWW-Authenticate", "Bearer realm=\"hdim-api\", error=\"invalid_token\", error_description=\"" +
                authException.getMessage() + "\"");

        // Build error response
        Map<String, Object> body = buildErrorResponse(request, authException);

        // Write response body
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Builds structured error response JSON body.
     *
     * Response includes:
     * - error: Standard HTTP error name
     * - message: Detailed error message from exception
     * - path: Request path for debugging
     * - timestamp: Current timestamp for correlation
     *
     * @param request HTTP request for path extraction
     * @param exception Authentication exception with error details
     * @return Map containing error response fields
     */
    private Map<String, Object> buildErrorResponse(
            HttpServletRequest request,
            AuthenticationException exception) {

        Map<String, Object> response = new HashMap<>();
        response.put("error", "Unauthorized");
        response.put("message", exception.getMessage());
        response.put("path", request.getRequestURI());
        response.put("timestamp", System.currentTimeMillis());

        // Add cause if available (helps with debugging)
        if (exception.getCause() != null) {
            response.put("cause", exception.getCause().getMessage());
        }

        return response;
    }
}

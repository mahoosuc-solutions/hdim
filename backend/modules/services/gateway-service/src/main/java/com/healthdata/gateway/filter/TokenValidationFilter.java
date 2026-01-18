package com.healthdata.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.gateway.service.TokenRevocationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Token Validation Filter (Phase 2.0 Team 3.3)
 *
 * Validates access tokens against Redis blacklist before allowing request processing.
 * Rejects requests with revoked tokens (401 Unauthorized).
 *
 * Features:
 * - Extracts Bearer tokens from Authorization header
 * - Parses JWT to extract JTI (JWT ID)
 * - Checks token against Redis blacklist
 * - Fails open: allows request if service unavailable
 * - Multi-tenant isolation support
 * - Proper error responses with descriptive messages
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenValidationFilter extends OncePerRequestFilter {

    private final TokenRevocationService tokenRevocationService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract Bearer token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                // No token provided, allow request to proceed
                filterChain.doFilter(request, response);
                return;
            }

            // Extract token from "Bearer <token>" format
            String token = extractBearerToken(authHeader);
            if (token == null || token.isEmpty()) {
                // Malformed header, allow to proceed (other filters will handle auth)
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JTI from JWT token
            String jti = extractJTIFromToken(token);
            if (jti == null || jti.isEmpty()) {
                // Could not extract JTI, use full token as identifier
                jti = token;
            }

            // Check if token is blacklisted
            if (tokenRevocationService.isBlacklisted(jti)) {
                // Token is revoked, return 401 Unauthorized
                log.warn("Access denied: Token is revoked (JTI: {})", jti);
                sendUnauthorizedResponse(response, "Token has been revoked");
                return;
            }

            // Token is valid, allow request to proceed
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Fail open: if service is unavailable, allow request to proceed
            // (other security layers should catch unauthorized requests)
            log.error("Error during token validation", e);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract Bearer token from Authorization header
     *
     * Supports both "Bearer token" and "bearer token" formats
     *
     * @param authHeader Authorization header value
     * @return token value or null if invalid
     */
    private String extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return null;
        }

        String lowerHeader = authHeader.toLowerCase();
        if (!lowerHeader.startsWith("bearer ")) {
            return null;
        }

        return authHeader.substring(7).trim();
    }

    /**
     * Extract JTI (JWT ID) from JWT token
     *
     * Parses the JWT payload to extract the "jti" claim.
     * JWT format: header.payload.signature where each part is Base64-encoded
     *
     * @param token JWT token
     * @return JTI value or null if cannot extract
     */
    private String extractJTIFromToken(String token) {
        try {
            // Split JWT into parts
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }

            // Decode payload (second part)
            String payload = parts[1];

            // Add padding if needed
            int padLength = 4 - (payload.length() % 4);
            if (padLength != 4) {
                payload += "=".repeat(padLength);
            }

            // Decode Base64
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decoded, StandardCharsets.UTF_8);

            // Parse JSON to extract JTI
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
            return (String) claims.get("jti");

        } catch (Exception e) {
            log.debug("Could not extract JTI from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Send 401 Unauthorized response
     *
     * @param response HttpServletResponse
     * @param message error message
     * @throws IOException if error writing response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, String> error = new HashMap<>();
        error.put("error", "UNAUTHORIZED");
        error.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}

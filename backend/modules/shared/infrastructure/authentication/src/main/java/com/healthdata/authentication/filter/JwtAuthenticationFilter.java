package com.healthdata.authentication.filter;

import com.healthdata.authentication.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter for validating and processing JWT tokens.
 *
 * This filter intercepts all HTTP requests and:
 * 1. Extracts JWT token from Authorization header (Bearer scheme)
 * 2. Validates the token using JwtTokenService
 * 3. Extracts user information and authorities from token claims
 * 4. Creates Spring Security Authentication object
 * 5. Sets authentication in SecurityContext
 *
 * Filter Behavior:
 * - Runs once per request (OncePerRequestFilter)
 * - Supports both JWT and Basic Auth (backward compatibility)
 * - Handles expired, invalid, and malformed tokens gracefully
 * - Does not block the filter chain on token failure
 * - Logs all authentication attempts for audit
 *
 * Token Format:
 * Authorization: Bearer <jwt_token>
 *
 * Security Considerations:
 * - Token validation is delegated to JwtTokenService
 * - Failed authentication does not block request (allows fallback to Basic Auth)
 * - SecurityContext is only set for valid tokens
 * - All exceptions are caught and logged
 */
@Slf4j
@Component
@Profile("!test")  // Don't load in test profile - tests use permitAll() security
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final JwtTokenService jwtTokenService;

    /**
     * Filter internal implementation.
     * Processes JWT token and sets authentication in SecurityContext.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Extract JWT token from request
            String jwt = extractJwtFromRequest(request);

            // Process JWT if present
            if (jwt != null && !jwt.isBlank()) {
                processJwtToken(jwt, request);
            } else {
                log.trace("No JWT token found in request, skipping JWT authentication");
            }

        } catch (Exception e) {
            log.error("Error processing JWT authentication", e);
            // Don't block the filter chain - allow fallback to Basic Auth
            SecurityContextHolder.clearContext();
        }

        // Continue filter chain regardless of authentication result
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * Expects header format: "Authorization: Bearer <token>"
     *
     * @param request HTTP request
     * @return JWT token string, or null if not found
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX_LENGTH);
        }

        return null;
    }

    /**
     * Process and validate JWT token.
     * If valid, creates Authentication and sets in SecurityContext.
     *
     * @param jwt JWT token string
     * @param request HTTP request for authentication details
     */
    private void processJwtToken(String jwt, HttpServletRequest request) {
        try {
            // Validate token
            if (!jwtTokenService.validateToken(jwt)) {
                log.debug("JWT token validation failed");
                return;
            }

            // Extract user information from token
            String username = jwtTokenService.extractUsername(jwt);
            Set<String> roles = jwtTokenService.extractRoles(jwt);
            Set<String> tenantIds = jwtTokenService.extractTenantIds(jwt);

            log.debug("JWT token validated for user: {}, roles: {}, tenants: {}",
                username, roles, tenantIds);

            // Check if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.trace("User already authenticated, skipping JWT authentication");
                return;
            }

            // Convert roles to Spring Security authorities
            Set<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

            // Set additional details (IP address, session info, etc.)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authentication successful for user: {}", username);

            // Store tenant IDs in request attribute for TenantAccessFilter
            request.setAttribute("userTenantIds", tenantIds);

        } catch (Exception e) {
            log.warn("Failed to process JWT token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Determine if this filter should be applied to the given request.
     * Can be overridden to skip filter for specific paths (e.g., /public, /health).
     *
     * @param request HTTP request
     * @return true if filter should be applied, false to skip
     * @throws ServletException if servlet error occurs
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip JWT filter for public endpoints that don't need authentication
        // Health checks, metrics, and API docs should be accessible without tokens
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/quality-measure/api") ||
               path.startsWith("/api") ||
               path.startsWith("/patient-health") ||
               path.startsWith("/mental-health") ||
               path.startsWith("/care-gaps") ||
               path.startsWith("/risk-stratification") ||
               path.equals("/favicon.ico");
    }
}

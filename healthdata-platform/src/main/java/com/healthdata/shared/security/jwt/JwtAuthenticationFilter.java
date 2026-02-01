package com.healthdata.shared.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter - Validates JWT tokens and sets SecurityContext
 *
 * Extends OncePerRequestFilter to ensure the filter is applied only once per request.
 * Extracts JWT token from Authorization header (Bearer scheme).
 * Validates token and establishes authentication in SecurityContext if valid.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    /**
     * Filter method executed for each HTTP request
     *
     * Attempts to extract and validate JWT token from request header.
     * If valid, authenticates the user and sets SecurityContext.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     * @param filterChain Filter chain for continuing request processing
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractTokenFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                authenticateUser(jwt);
                log.debug("Successfully authenticated user from JWT token");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     *
     * Expects "Authorization: Bearer <token>" format
     * Handles malformed headers gracefully
     *
     * @param request HTTP servlet request
     * @return JWT token or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX_LENGTH);
            log.debug("Extracted JWT token from Authorization header");
            return token;
        }

        return null;
    }

    /**
     * Authenticate user based on JWT token and set SecurityContext
     *
     * Extracts username and roles from token and creates authentication object.
     * SecurityContext is set with this authentication for downstream processing.
     *
     * @param token Valid JWT token
     */
    private void authenticateUser(String jwt) {
        String username = tokenProvider.getUsernameFromToken(jwt);
        List<String> roles = tokenProvider.getRolesFromToken(jwt);
        String tenantId = tokenProvider.getTenantIdFromToken(jwt);
        String userId = tokenProvider.getUserIdFromToken(jwt);

        // Convert roles to Spring Security authorities
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // Create authentication token
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

        // Store additional information in details (optional)
        authentication.setDetails(new JwtAuthenticationDetails(jwt, userId, tenantId));

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("User {} authenticated with roles: {}", username, roles);
    }

    /**
     * Determine if filter should be applied to this request
     *
     * Typically filters public endpoints that don't require authentication.
     * Override this method to customize which endpoints bypass JWT authentication.
     *
     * @param request HTTP servlet request
     * @return true if filter should be skipped for this request
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Define paths that should bypass JWT authentication
        String[] publicPaths = {
                "/actuator/health",
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/swagger-ui",
                "/v3/api-docs",
                "/api-docs"
        };

        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                log.debug("Skipping JWT filter for public path: {}", path);
                return true;
            }
        }

        return false;
    }
}

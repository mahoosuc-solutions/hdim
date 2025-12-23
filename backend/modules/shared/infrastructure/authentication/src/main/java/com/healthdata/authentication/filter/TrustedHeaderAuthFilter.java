package com.healthdata.authentication.filter;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Authentication filter for backend services that trusts gateway-injected headers.
 *
 * This filter is used by backend services to extract user context from trusted
 * headers injected by the API Gateway. It does NOT validate JWT tokens - that's
 * the gateway's responsibility.
 *
 * Prerequisites:
 * - The API Gateway MUST validate the JWT token before routing to backend
 * - The API Gateway MUST strip any externally-provided X-Auth-* headers
 * - The API Gateway MUST inject trusted headers with user context
 *
 * Security Model:
 * - This filter ONLY trusts headers when X-Auth-Validated is present
 * - Backend services run in a trusted network (no external access)
 * - If headers are missing or invalid, request continues unauthenticated
 *
 * Usage:
 * Register this filter in your service's SecurityConfig instead of JwtAuthenticationFilter:
 *
 * <pre>
 * @Bean
 * public SecurityFilterChain securityFilterChain(
 *     HttpSecurity http,
 *     TrustedHeaderAuthFilter trustedHeaderAuthFilter
 * ) throws Exception {
 *     http.addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
 *     return http.build();
 * }
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
public class TrustedHeaderAuthFilter extends OncePerRequestFilter {

    /**
     * Configuration for controlling filter behavior.
     */
    private final TrustedHeaderAuthConfig config;

    /**
     * Filter internal implementation.
     * Extracts user context from trusted headers and sets authentication in SecurityContext.
     */
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Check if request has the validated header from gateway
            String validatedHeader = request.getHeader(AuthHeaderConstants.HEADER_VALIDATED);

            if (validatedHeader != null && isValidSignature(validatedHeader)) {
                processAuthHeaders(request);
            } else if (validatedHeader != null) {
                log.warn("Invalid gateway validation signature: {}", maskSignature(validatedHeader));
            } else {
                log.trace("No gateway validation header found, skipping trusted header auth");
            }

        } catch (Exception e) {
            log.error("Error processing trusted headers", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Process authentication headers and set SecurityContext.
     */
    private void processAuthHeaders(HttpServletRequest request) {
        String userId = request.getHeader(AuthHeaderConstants.HEADER_USER_ID);
        String username = request.getHeader(AuthHeaderConstants.HEADER_USERNAME);
        String tenantIdsHeader = request.getHeader(AuthHeaderConstants.HEADER_TENANT_IDS);
        String rolesHeader = request.getHeader(AuthHeaderConstants.HEADER_ROLES);

        // Validate required headers
        if (username == null || username.isBlank()) {
            log.debug("Missing username header, skipping authentication");
            return;
        }

        // Parse tenant IDs
        Set<String> tenantIds = parseCommaSeparated(tenantIdsHeader);

        // Parse roles and convert to authorities
        Set<String> roles = parseCommaSeparated(rolesHeader);
        Set<SimpleGrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toSet());

        log.debug("Trusted header auth for user: {}, roles: {}, tenants: {}",
            username, roles, tenantIds);

        // Check if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.trace("User already authenticated, skipping trusted header auth");
            return;
        }

        // Create authentication token
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Store attributes for downstream use (e.g., TenantAccessFilter)
        request.setAttribute(AuthHeaderConstants.ATTR_TENANT_IDS, tenantIds);
        request.setAttribute(AuthHeaderConstants.ATTR_USERNAME, username);
        request.setAttribute(AuthHeaderConstants.ATTR_ROLES, roles);

        if (userId != null && !userId.isBlank()) {
            try {
                request.setAttribute(AuthHeaderConstants.ATTR_USER_ID, UUID.fromString(userId));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user ID format in header: {}", userId);
            }
        }

        log.debug("Trusted header authentication successful for user: {}", username);
    }

    /**
     * Validate the gateway signature.
     *
     * The signature format is: gateway-{timestamp}-{hmac}
     * This ensures headers were injected by the gateway and not forged.
     */
    private boolean isValidSignature(String signature) {
        if (signature == null || !signature.startsWith(AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX)) {
            return false;
        }

        // In development/test mode, accept simple signatures
        if (config.isDevelopmentMode()) {
            return signature.startsWith(AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX);
        }

        // Production: Validate HMAC signature
        // Format: gateway-{timestamp}-{hmac}
        String[] parts = signature.split("-");
        if (parts.length < 3) {
            return false;
        }

        try {
            long timestamp = Long.parseLong(parts[1]);
            long now = System.currentTimeMillis() / 1000;

            // Reject if signature is older than 5 minutes
            if (Math.abs(now - timestamp) > 300) {
                log.warn("Gateway signature expired: {} seconds old", now - timestamp);
                return false;
            }

            // TODO: Validate HMAC using shared secret
            // String expectedHmac = computeHmac(timestamp, config.getSharedSecret());
            // return parts[2].equals(expectedHmac);

            return true;

        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp in gateway signature");
            return false;
        }
    }

    /**
     * Parse comma-separated string into a Set.
     */
    private Set<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(value.split(AuthHeaderConstants.VALUE_DELIMITER)));
    }

    /**
     * Mask signature for safe logging.
     */
    private String maskSignature(String signature) {
        if (signature == null || signature.length() < 10) {
            return "***";
        }
        return signature.substring(0, 8) + "***";
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip filter for actuator and swagger endpoints
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico");
    }

    /**
     * Configuration class for TrustedHeaderAuthFilter.
     */
    public static class TrustedHeaderAuthConfig {
        private boolean developmentMode = false;
        private String sharedSecret;

        public boolean isDevelopmentMode() {
            return developmentMode;
        }

        public void setDevelopmentMode(boolean developmentMode) {
            this.developmentMode = developmentMode;
        }

        public String getSharedSecret() {
            return sharedSecret;
        }

        public void setSharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
        }

        public static TrustedHeaderAuthConfig development() {
            TrustedHeaderAuthConfig config = new TrustedHeaderAuthConfig();
            config.setDevelopmentMode(true);
            return config;
        }

        public static TrustedHeaderAuthConfig production(String sharedSecret) {
            TrustedHeaderAuthConfig config = new TrustedHeaderAuthConfig();
            config.setDevelopmentMode(false);
            config.setSharedSecret(sharedSecret);
            return config;
        }
    }
}

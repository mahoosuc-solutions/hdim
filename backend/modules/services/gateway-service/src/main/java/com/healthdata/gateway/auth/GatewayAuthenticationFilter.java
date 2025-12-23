package com.healthdata.gateway.auth;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.service.JwtTokenService;
import com.healthdata.gateway.config.GatewayAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gateway Authentication Filter - Central authentication point for all requests.
 *
 * This filter:
 * 1. Strips any externally-provided X-Auth-* headers (security)
 * 2. Extracts and validates JWT tokens from Authorization header
 * 3. Injects trusted headers for downstream services:
 *    - X-Auth-User-Id: User's unique identifier
 *    - X-Auth-Username: User's username
 *    - X-Auth-Tenant-Ids: Comma-separated tenant IDs
 *    - X-Auth-Roles: Comma-separated role names
 *    - X-Auth-Validated: HMAC signature proving headers came from gateway
 *    - X-Auth-Token-Id: Original JWT ID for audit/revocation
 *    - X-Auth-Token-Expires: Token expiration timestamp
 *
 * Security Features:
 * - Strips external auth headers before processing (prevents header injection)
 * - HMAC signature on validated header prevents forgery
 * - Logs all authentication attempts for audit
 * - Supports public paths that bypass authentication
 *
 * Order: -100 (runs very early in filter chain)
 */
@Slf4j
@Component
@Order(-100)
@RequiredArgsConstructor
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JwtTokenService jwtTokenService;
    private final GatewayAuthProperties authProperties;
    private final PublicPathRegistry publicPathRegistry;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Check if authentication is enabled
        if (!authProperties.getEnabled()) {
            log.trace("Gateway authentication disabled, passing through");
            filterChain.doFilter(request, response);
            return;
        }

        // Strip external auth headers (SECURITY: prevent header injection attacks)
        HttpServletRequest sanitizedRequest = stripExternalAuthHeaders(request);

        // Check if path is public
        if (publicPathRegistry.isPublicPath(path)) {
            log.trace("Public path {}, skipping authentication", path);
            filterChain.doFilter(sanitizedRequest, response);
            return;
        }

        try {
            // Extract JWT from request
            String jwt = extractJwtFromRequest(sanitizedRequest);

            if (jwt == null || jwt.isBlank()) {
                handleMissingToken(sanitizedRequest, response, filterChain, path);
                return;
            }

            // Validate token
            if (!jwtTokenService.validateToken(jwt)) {
                handleInvalidToken(response, path);
                return;
            }

            // Extract claims from token
            String username = jwtTokenService.extractUsername(jwt);
            UUID userId = jwtTokenService.extractUserId(jwt);
            Set<String> tenantIds = jwtTokenService.extractTenantIds(jwt);
            Set<String> roles = jwtTokenService.extractRoles(jwt);
            String tokenId = jwtTokenService.extractJwtId(jwt);
            Date tokenExpires = jwtTokenService.getExpirationDate(jwt);

            log.debug("JWT validated for user: {}, path: {}, method: {}", username, path, method);

            // Create wrapper request with injected auth headers
            HttpServletRequest authenticatedRequest = injectAuthHeaders(
                sanitizedRequest, userId, username, tenantIds, roles, tokenId, tokenExpires
            );

            // Set Spring Security context
            setSecurityContext(username, roles, authenticatedRequest);

            // Log authentication event for audit
            if (authProperties.getAuditLogging()) {
                logAuthenticationEvent(username, userId, path, method, "SUCCESS");
            }

            filterChain.doFilter(authenticatedRequest, response);

        } catch (Exception e) {
            log.error("Authentication error for path {}: {}", path, e.getMessage());
            handleAuthenticationError(response, e);
        }
    }

    /**
     * Strip any externally-provided X-Auth-* headers from the request.
     * SECURITY: Prevents header injection attacks.
     */
    private HttpServletRequest stripExternalAuthHeaders(HttpServletRequest request) {
        if (!authProperties.getStripExternalAuthHeaders()) {
            return request;
        }

        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (AuthHeaderConstants.isAuthHeader(name)) {
                    return null; // Strip auth headers
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (AuthHeaderConstants.isAuthHeader(name)) {
                    return Collections.emptyEnumeration();
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>();
                Enumeration<String> originalNames = super.getHeaderNames();
                while (originalNames.hasMoreElements()) {
                    String name = originalNames.nextElement();
                    if (!AuthHeaderConstants.isAuthHeader(name)) {
                        names.add(name);
                    }
                }
                return Collections.enumeration(names);
            }
        };
    }

    /**
     * Inject authenticated user context headers into request.
     */
    private HttpServletRequest injectAuthHeaders(
        HttpServletRequest request,
        UUID userId,
        String username,
        Set<String> tenantIds,
        Set<String> roles,
        String tokenId,
        Date tokenExpires
    ) {
        // Generate validation signature
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateValidationSignature(userId.toString(), timestamp);

        // Create header map
        Map<String, String> injectedHeaders = new HashMap<>();
        injectedHeaders.put(AuthHeaderConstants.HEADER_USER_ID, userId.toString());
        injectedHeaders.put(AuthHeaderConstants.HEADER_USERNAME, username);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TENANT_IDS, String.join(",", tenantIds));
        injectedHeaders.put(AuthHeaderConstants.HEADER_ROLES, String.join(",", roles));
        injectedHeaders.put(AuthHeaderConstants.HEADER_VALIDATED, signature);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TOKEN_ID, tokenId);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TOKEN_EXPIRES, String.valueOf(tokenExpires.getTime() / 1000));

        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if (injectedHeaders.containsKey(name)) {
                    return injectedHeaders.get(name);
                }
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if (injectedHeaders.containsKey(name)) {
                    return Collections.enumeration(List.of(injectedHeaders.get(name)));
                }
                return super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                Set<String> names = new LinkedHashSet<>();
                Enumeration<String> originalNames = super.getHeaderNames();
                while (originalNames.hasMoreElements()) {
                    names.add(originalNames.nextElement());
                }
                names.addAll(injectedHeaders.keySet());
                return Collections.enumeration(names);
            }
        };
    }

    /**
     * Generate HMAC signature for validation header.
     * Format: gateway-{timestamp}-{hmac}
     */
    private String generateValidationSignature(String userId, long timestamp) {
        String data = userId + ":" + timestamp;

        try {
            String secret = authProperties.getHeaderSigningSecret();
            if (secret == null || secret.isBlank()) {
                // Development mode: simple signature
                return AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX + timestamp + "-dev";
            }

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String hmac = Base64.getEncoder().encodeToString(hmacBytes);

            return AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX + timestamp + "-" + hmac;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating validation signature", e);
            return AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX + timestamp + "-error";
        }
    }

    /**
     * Set Spring Security context with authenticated user.
     */
    private void setSecurityContext(String username, Set<String> roles, HttpServletRequest request) {
        Set<SimpleGrantedAuthority> authorities = roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toSet());

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(username, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Extract JWT from Authorization header.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX_LENGTH);
        }
        return null;
    }

    /**
     * Handle missing token.
     */
    private void handleMissingToken(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain,
        String path
    ) throws IOException, ServletException {

        if (!authProperties.getEnforced()) {
            // Non-enforced mode: allow request without auth (demo mode)
            log.debug("No token provided for {}, but auth not enforced", path);
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("No token provided for protected path: {}", path);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}");
    }

    /**
     * Handle invalid token.
     */
    private void handleInvalidToken(HttpServletResponse response, String path) throws IOException {
        log.debug("Invalid token for path: {}", path);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Invalid or expired token\"}");
    }

    /**
     * Handle authentication error.
     */
    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"authentication_error\",\"message\":\"Authentication processing failed\"}");
    }

    /**
     * Log authentication event for audit.
     */
    private void logAuthenticationEvent(
        String username,
        UUID userId,
        String path,
        String method,
        String result
    ) {
        log.info("AUTH_EVENT: user={}, userId={}, path={}, method={}, result={}",
            username, userId, path, method, result);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // This filter should run for all requests to:
        // 1. Strip external auth headers (security)
        // 2. Check if path is public
        // 3. Validate tokens for protected paths
        return false;
    }
}

package com.healthdata.gateway.auth;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.entity.ApiKey;
import com.healthdata.authentication.service.ApiKeyService;
import com.healthdata.authentication.service.CookieService;
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
import org.springframework.beans.factory.ObjectProvider;
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
 * 2. Extracts and validates JWT tokens from Authorization header OR HttpOnly cookies
 * 3. Injects trusted headers for downstream services:
 *    - X-Auth-User-Id: User's unique identifier
 *    - X-Auth-Username: User's username
 *    - X-Auth-Tenant-Ids: Comma-separated tenant IDs
 *    - X-Auth-Roles: Comma-separated role names
 *    - X-Auth-Validated: HMAC signature proving headers came from gateway
 *    - X-Auth-Token-Id: Original JWT ID for audit/revocation
 *    - X-Auth-Token-Expires: Token expiration timestamp
 *
 * Token Sources (in priority order):
 * 1. Authorization: Bearer <jwt_token>  (for API clients)
 * 2. HttpOnly Cookie: hdim_access_token (for browser clients - XSS protected)
 *
 * Security Features:
 * - Strips external auth headers before processing (prevents header injection)
 * - HMAC signature on validated header prevents forgery
 * - Logs all authentication attempts for audit
 * - Supports public paths that bypass authentication
 * - HttpOnly cookies provide XSS protection
 *
 * Order: -100 (runs very early in filter chain)
 */
@Slf4j
@Component
@Order(-100)
@RequiredArgsConstructor
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JwtTokenService jwtTokenService;
    private final GatewayAuthProperties authProperties;
    private final PublicPathRegistry publicPathRegistry;
    private final CookieService cookieService;
    private final ObjectProvider<ApiKeyService> apiKeyServiceProvider;

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
                String rawApiKey = extractApiKeyFromRequest(sanitizedRequest);
                if (rawApiKey != null && !rawApiKey.isBlank()) {
                    if (!handleApiKeyAuthentication(sanitizedRequest, response, filterChain, path, method, rawApiKey)) {
                        return;
                    }
                    return;
                }
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

    private boolean handleApiKeyAuthentication(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain,
        String path,
        String method,
        String rawApiKey
    ) throws IOException, ServletException {
        ApiKeyService apiKeyService = apiKeyServiceProvider.getIfAvailable();
        if (apiKeyService == null) {
            log.warn("API key provided but ApiKeyService is not available");
            handleInvalidToken(response, path);
            return false;
        }

        String clientIp = extractClientIp(request);
        Optional<ApiKey> apiKeyOpt = apiKeyService.validateApiKey(rawApiKey, clientIp);
        if (apiKeyOpt.isEmpty()) {
            log.debug("Invalid API key for path: {}", path);
            handleInvalidToken(response, path);
            return false;
        }

        ApiKey apiKey = apiKeyOpt.get();
        String username = "api-key:" + apiKey.getKeyPrefix();
        Set<String> roles = Set.of("API_CLIENT");
        Set<String> tenantIds = Set.of(apiKey.getTenantId());
        String tokenId = "api-key-" + apiKey.getId();
        Date tokenExpires = apiKey.getExpiresAt() != null
            ? Date.from(apiKey.getExpiresAt())
            : new Date(System.currentTimeMillis() + 31536000000L);

        HttpServletRequest authenticatedRequest = injectApiKeyAuthHeaders(
            request, apiKey, username, tenantIds, roles, tokenId, tokenExpires
        );
        setSecurityContext(username, roles, authenticatedRequest);

        if (authProperties.getAuditLogging()) {
            log.info("AUTH_EVENT: user={}, userId={}, path={}, method={}, result=API_KEY_SUCCESS",
                username, apiKey.getCreatedBy(), path, method);
        }

        filterChain.doFilter(authenticatedRequest, response);
        return true;
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

    private HttpServletRequest injectApiKeyAuthHeaders(
        HttpServletRequest request,
        ApiKey apiKey,
        String username,
        Set<String> tenantIds,
        Set<String> roles,
        String tokenId,
        Date tokenExpires
    ) {
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateValidationSignature(apiKey.getCreatedBy().toString(), timestamp);

        Map<String, String> injectedHeaders = new HashMap<>();
        injectedHeaders.put(AuthHeaderConstants.HEADER_USER_ID, apiKey.getCreatedBy().toString());
        injectedHeaders.put(AuthHeaderConstants.HEADER_USERNAME, username);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TENANT_IDS, String.join(",", tenantIds));
        injectedHeaders.put(AuthHeaderConstants.HEADER_ROLES, String.join(",", roles));
        injectedHeaders.put(AuthHeaderConstants.HEADER_VALIDATED, signature);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TOKEN_ID, tokenId);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TOKEN_EXPIRES, String.valueOf(tokenExpires.getTime() / 1000));
        injectedHeaders.put(AuthHeaderConstants.HEADER_API_KEY_ID, apiKey.getId().toString());
        if (apiKey.getRateLimitPerMinute() != null && apiKey.getRateLimitPerMinute() > 0) {
            injectedHeaders.put(AuthHeaderConstants.HEADER_API_KEY_RATE_LIMIT,
                String.valueOf(apiKey.getRateLimitPerMinute()));
        }

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
     * Extract JWT from request.
     *
     * Priority:
     * 1. Authorization header: "Bearer <token>" (for API clients)
     * 2. HttpOnly cookie: hdim_access_token (for browser clients)
     *
     * @param request HTTP request
     * @return JWT token string, or null if not found
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // First check Authorization header (higher priority for API clients)
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            log.trace("JWT found in Authorization header");
            return authHeader.substring(BEARER_PREFIX_LENGTH);
        }

        // Fall back to HttpOnly cookie (for browser clients with XSS protection)
        return cookieService.getAccessTokenFromCookie(request)
            .map(token -> {
                log.trace("JWT found in HttpOnly cookie");
                return token;
            })
            .orElse(null);
    }

    private String extractApiKeyFromRequest(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex > 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Handle missing token.
     * In demo mode (enforced=false), injects demo user context headers.
     */
    private void handleMissingToken(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain,
        String path
    ) throws IOException, ServletException {

        if (!authProperties.getEnforced()) {
            // Demo mode: inject demo user headers for unauthenticated requests
            log.debug("No token for {}, injecting demo user context (auth not enforced)", path);

            GatewayAuthProperties.DemoUserConfig demoUser = authProperties.getDemoUser();

            // Create demo user context with injected headers
            HttpServletRequest demoRequest = injectDemoAuthHeaders(
                request,
                demoUser.getUserId(),
                demoUser.getUsername(),
                new HashSet<>(demoUser.getTenantIds()),
                new HashSet<>(demoUser.getRoles())
            );

            // Set Spring Security context for demo user
            setSecurityContext(demoUser.getUsername(), new HashSet<>(demoUser.getRoles()), demoRequest);

            // Log demo mode access for audit
            if (authProperties.getAuditLogging()) {
                log.info("AUTH_EVENT: user={}, userId={}, path={}, method={}, result=DEMO_MODE",
                    demoUser.getUsername(), demoUser.getUserId(), path, request.getMethod());
            }

            filterChain.doFilter(demoRequest, response);
            return;
        }

        log.debug("No token provided for protected path: {}", path);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}");
    }

    /**
     * Inject demo user auth headers (used when auth not enforced).
     * Similar to injectAuthHeaders but uses string userId and no token info.
     */
    private HttpServletRequest injectDemoAuthHeaders(
        HttpServletRequest request,
        String userId,
        String username,
        Set<String> tenantIds,
        Set<String> roles
    ) {
        // Generate validation signature
        long timestamp = System.currentTimeMillis() / 1000;
        String signature = generateValidationSignature(userId, timestamp);

        // Create header map
        Map<String, String> injectedHeaders = new HashMap<>();
        injectedHeaders.put(AuthHeaderConstants.HEADER_USER_ID, userId);
        injectedHeaders.put(AuthHeaderConstants.HEADER_USERNAME, username);
        injectedHeaders.put(AuthHeaderConstants.HEADER_TENANT_IDS, String.join(",", tenantIds));
        injectedHeaders.put(AuthHeaderConstants.HEADER_ROLES, String.join(",", roles));
        injectedHeaders.put(AuthHeaderConstants.HEADER_VALIDATED, signature);
        // Demo mode doesn't have token info
        injectedHeaders.put(AuthHeaderConstants.HEADER_TOKEN_ID, "demo-token");
        injectedHeaders.put(AuthHeaderConstants.HEADER_TOKEN_EXPIRES, String.valueOf(timestamp + 86400)); // 24h

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

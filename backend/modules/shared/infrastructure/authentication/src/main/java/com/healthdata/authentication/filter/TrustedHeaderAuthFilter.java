package com.healthdata.authentication.filter;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
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

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long DEFAULT_SIGNATURE_VALIDITY_SECONDS = 300; // 5 minutes

    /**
     * Configuration for controlling filter behavior.
     */
    private final TrustedHeaderAuthConfig config;

    /**
     * Metrics registry for collecting authentication metrics.
     */
    private final MeterRegistry meterRegistry;

    /**
     * Metrics counters and timers.
     */
    private Counter authSuccessCounter;
    private Counter authFailureCounter;
    private Counter hmacValidationFailureCounter;
    private Timer authLatencyTimer;

    /**
     * Initialize metrics after construction.
     */
    @Override
    public void afterPropertiesSet() throws ServletException {
        super.afterPropertiesSet();
        initializeMetrics();
    }

    /**
     * Initialize Micrometer metrics for authentication tracking.
     */
    private void initializeMetrics() {
        authSuccessCounter = Counter.builder("auth_success_total")
            .description("Total number of successful authentications")
            .tag("filter", "trusted_header_auth")
            .register(meterRegistry);

        authFailureCounter = Counter.builder("auth_failure_total")
            .description("Total number of failed authentications")
            .tag("filter", "trusted_header_auth")
            .register(meterRegistry);

        hmacValidationFailureCounter = Counter.builder("hmac_validation_failures_total")
            .description("Total number of HMAC validation failures")
            .tag("filter", "trusted_header_auth")
            .register(meterRegistry);

        authLatencyTimer = Timer.builder("auth_latency")
            .description("Authentication processing latency in seconds")
            .tag("filter", "trusted_header_auth")
            .register(meterRegistry);

        log.info("Metrics initialized for TrustedHeaderAuthFilter");
    }

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

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Check if request has the validated header from gateway
            String validatedHeader = request.getHeader(AuthHeaderConstants.HEADER_VALIDATED);
            String userId = request.getHeader(AuthHeaderConstants.HEADER_USER_ID);

            if (validatedHeader != null && isValidSignature(validatedHeader, userId)) {
                processAuthHeaders(request);
                authSuccessCounter.increment();
                sample.stop(authLatencyTimer);
            } else if (validatedHeader != null) {
                log.warn("Invalid gateway validation signature for request to {}: {}",
                    request.getRequestURI(), maskSignature(validatedHeader));
                hmacValidationFailureCounter.increment();
                authFailureCounter.increment();
                sample.stop(authLatencyTimer);
                // In strict mode, reject the request
                if (config.isStrictMode()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"unauthorized\",\"message\":\"Invalid gateway signature\"}"
                    );
                    return;
                }
            } else {
                log.trace("No gateway validation header found, skipping trusted header auth");
                sample.stop(authLatencyTimer);
            }

        } catch (Exception e) {
            log.error("Error processing trusted headers", e);
            authFailureCounter.increment();
            sample.stop(authLatencyTimer);
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
     *
     * @param signature the X-Auth-Validated header value
     * @param userId the user ID from X-Auth-User-Id header (used in HMAC computation)
     * @return true if signature is valid
     */
    private boolean isValidSignature(String signature, String userId) {
        if (signature == null || !signature.startsWith(AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX)) {
            log.debug("Signature missing or invalid prefix");
            return false;
        }

        // In development/test mode, accept simple signatures
        if (config.isDevelopmentMode()) {
            log.trace("Development mode: accepting signature with valid prefix");
            return signature.startsWith(AuthHeaderConstants.VALIDATED_SIGNATURE_PREFIX);
        }

        // Production: Validate HMAC signature
        // Format: gateway-{timestamp}-{hmac}
        String[] parts = signature.split("-", 3);
        if (parts.length < 3) {
            log.warn("Invalid signature format: expected gateway-{timestamp}-{hmac}");
            return false;
        }

        try {
            long timestamp = Long.parseLong(parts[1]);
            String providedHmac = parts[2];
            long now = System.currentTimeMillis() / 1000;
            long validitySeconds = config.getSignatureValiditySeconds();

            // Reject if signature is expired
            if (Math.abs(now - timestamp) > validitySeconds) {
                log.warn("Gateway signature expired: {} seconds old (max: {})",
                    now - timestamp, validitySeconds);
                return false;
            }

            // Validate HMAC
            String sharedSecret = config.getSharedSecret();
            if (sharedSecret == null || sharedSecret.isBlank()) {
                log.error("SECURITY: Shared secret not configured for HMAC validation");
                return false;
            }

            if (userId == null || userId.isBlank()) {
                log.warn("User ID missing, cannot validate HMAC");
                return false;
            }

            // Compute expected HMAC (same algorithm as gateway)
            String expectedHmac = computeHmac(userId, timestamp, sharedSecret);
            if (expectedHmac == null) {
                log.error("Failed to compute HMAC for validation");
                return false;
            }

            // Constant-time comparison to prevent timing attacks
            if (!constantTimeEquals(expectedHmac, providedHmac)) {
                log.warn("HMAC validation failed for user: {}", maskUserId(userId));
                return false;
            }

            log.trace("HMAC validation successful for user: {}", maskUserId(userId));
            return true;

        } catch (NumberFormatException e) {
            log.warn("Invalid timestamp in gateway signature");
            return false;
        }
    }

    /**
     * Compute HMAC using the same algorithm as the gateway.
     *
     * @param userId the user ID
     * @param timestamp the signature timestamp
     * @param secret the shared secret
     * @return Base64-encoded HMAC or null on error
     */
    private String computeHmac(String userId, long timestamp, String secret) {
        try {
            String data = userId + ":" + timestamp;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error computing HMAC: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    /**
     * Mask user ID for safe logging.
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 8) {
            return "***";
        }
        return userId.substring(0, 8) + "***";
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
     *
     * Security Configuration:
     * - developmentMode: When true, accepts any signature with valid prefix (for local dev only)
     * - strictMode: When true, rejects requests with invalid signatures (production default)
     * - sharedSecret: HMAC secret shared with gateway (must match GATEWAY_AUTH_SIGNING_SECRET)
     * - signatureValiditySeconds: Max age of signature before rejection (default 300 = 5 min)
     */
    public static class TrustedHeaderAuthConfig {
        private boolean developmentMode = false;
        private boolean strictMode = false;
        private String sharedSecret;
        private long signatureValiditySeconds = 300; // 5 minutes

        public boolean isDevelopmentMode() {
            return developmentMode;
        }

        public void setDevelopmentMode(boolean developmentMode) {
            this.developmentMode = developmentMode;
        }

        public boolean isStrictMode() {
            return strictMode;
        }

        public void setStrictMode(boolean strictMode) {
            this.strictMode = strictMode;
        }

        public String getSharedSecret() {
            return sharedSecret;
        }

        public void setSharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
        }

        public long getSignatureValiditySeconds() {
            return signatureValiditySeconds;
        }

        public void setSignatureValiditySeconds(long signatureValiditySeconds) {
            this.signatureValiditySeconds = signatureValiditySeconds;
        }

        /**
         * Create development mode configuration (for local development only).
         * WARNING: Never use in production - bypasses HMAC validation.
         */
        public static TrustedHeaderAuthConfig development() {
            TrustedHeaderAuthConfig config = new TrustedHeaderAuthConfig();
            config.setDevelopmentMode(true);
            config.setStrictMode(false);
            return config;
        }

        /**
         * Create production configuration with HMAC validation.
         *
         * @param sharedSecret the HMAC secret (must match gateway's GATEWAY_AUTH_SIGNING_SECRET)
         * @return production configuration
         */
        public static TrustedHeaderAuthConfig production(String sharedSecret) {
            TrustedHeaderAuthConfig config = new TrustedHeaderAuthConfig();
            config.setDevelopmentMode(false);
            config.setStrictMode(true);
            config.setSharedSecret(sharedSecret);
            return config;
        }

        /**
         * Create production configuration with custom validity period.
         *
         * @param sharedSecret the HMAC secret (must match gateway's GATEWAY_AUTH_SIGNING_SECRET)
         * @param signatureValiditySeconds max age of signature in seconds
         * @return production configuration
         */
        public static TrustedHeaderAuthConfig production(String sharedSecret, long signatureValiditySeconds) {
            TrustedHeaderAuthConfig config = production(sharedSecret);
            config.setSignatureValiditySeconds(signatureValiditySeconds);
            return config;
        }

        /**
         * Validate configuration for production use.
         * @throws IllegalStateException if configuration is invalid for production
         */
        public void validateForProduction() {
            if (developmentMode) {
                throw new IllegalStateException(
                    "Development mode is enabled - not safe for production"
                );
            }
            if (sharedSecret == null || sharedSecret.isBlank()) {
                throw new IllegalStateException(
                    "Shared secret is required for production HMAC validation"
                );
            }
            if (sharedSecret.length() < 32) {
                throw new IllegalStateException(
                    "Shared secret must be at least 32 characters for production"
                );
            }
        }
    }
}

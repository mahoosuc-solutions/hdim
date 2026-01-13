package com.healthdata.testfixtures.security;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for generating Gateway Trust authentication headers in tests.
 * <p>
 * The HDIM platform uses a Gateway Trust authentication model where:
 * <ol>
 *   <li>Kong API Gateway validates JWT tokens at the edge</li>
 *   <li>Gateway Service injects trusted X-Auth-* headers</li>
 *   <li>Backend services trust these headers without re-validating JWT</li>
 * </ol>
 * <p>
 * This utility provides helper methods to generate the required headers for
 * integration tests without requiring a full authentication flow.
 * <p>
 * <h2>Header Overview</h2>
 * <table>
 *   <tr><th>Header</th><th>Description</th><th>Example</th></tr>
 *   <tr><td>X-Tenant-ID</td><td>Current tenant context</td><td>tenant-001</td></tr>
 *   <tr><td>X-Auth-User-Id</td><td>User's UUID</td><td>550e8400-...</td></tr>
 *   <tr><td>X-Auth-Username</td><td>User's login name</td><td>test.user@example.com</td></tr>
 *   <tr><td>X-Auth-Tenant-Ids</td><td>Comma-separated authorized tenants</td><td>tenant-001,tenant-002</td></tr>
 *   <tr><td>X-Auth-Roles</td><td>Comma-separated roles</td><td>ADMIN,EVALUATOR</td></tr>
 *   <tr><td>X-Auth-Validated</td><td>HMAC signature (production only)</td><td>base64(hmac)</td></tr>
 * </table>
 * <p>
 * <h2>Usage Examples</h2>
 * <h3>With MockMvc</h3>
 * <pre>{@code
 * mockMvc.perform(get("/api/v1/patients/123")
 *     .headers(GatewayTrustTestHeaders.adminHeaders("tenant-001")))
 *     .andExpect(status().isOk());
 *
 * // Or with custom user
 * mockMvc.perform(get("/api/v1/measures")
 *     .headers(GatewayTrustTestHeaders.builder()
 *         .tenantId("tenant-001")
 *         .userId("user-123")
 *         .roles("EVALUATOR", "VIEWER")
 *         .build()))
 *     .andExpect(status().isOk());
 * }</pre>
 * <p>
 * <h3>With WebTestClient</h3>
 * <pre>{@code
 * webTestClient.get()
 *     .uri("/api/v1/patients")
 *     .headers(headers -> GatewayTrustTestHeaders.applyHeaders(headers, "tenant-001", "ADMIN"))
 *     .exchange()
 *     .expectStatus().isOk();
 * }</pre>
 * <p>
 * <h3>RBAC Testing</h3>
 * <pre>{@code
 * // Test admin access
 * mockMvc.perform(post("/api/v1/measures")
 *     .headers(GatewayTrustTestHeaders.adminHeaders("tenant-001")))
 *     .andExpect(status().isCreated());
 *
 * // Test evaluator (should be forbidden for writes)
 * mockMvc.perform(post("/api/v1/measures")
 *     .headers(GatewayTrustTestHeaders.evaluatorHeaders("tenant-001")))
 *     .andExpect(status().isForbidden());
 *
 * // Test viewer (read-only)
 * mockMvc.perform(get("/api/v1/measures")
 *     .headers(GatewayTrustTestHeaders.viewerHeaders("tenant-001")))
 *     .andExpect(status().isOk());
 * }</pre>
 *
 * @see BaseTestContainersConfiguration
 * @since 1.0
 */
public final class GatewayTrustTestHeaders {

    // Header names
    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    public static final String AUTH_USER_ID_HEADER = "X-Auth-User-Id";
    public static final String AUTH_USERNAME_HEADER = "X-Auth-Username";
    public static final String AUTH_TENANT_IDS_HEADER = "X-Auth-Tenant-Ids";
    public static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    public static final String AUTH_VALIDATED_HEADER = "X-Auth-Validated";

    // HMAC algorithm
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // Default test signing secret (must be at least 32 characters)
    private static final String DEFAULT_TEST_SECRET = "test-signing-secret-for-development-only-32chars";

    private GatewayTrustTestHeaders() {
        // Utility class - prevent instantiation
    }

    // =========================================================================
    // Convenience Methods for Common Roles
    // =========================================================================

    /**
     * Creates headers for a SUPER_ADMIN user with access to all tenants.
     *
     * @param primaryTenantId the primary tenant ID for the request
     * @return HttpHeaders with super admin authentication
     */
    public static HttpHeaders superAdminHeaders(String primaryTenantId) {
        return builder()
                .tenantId(primaryTenantId)
                .userId(UUID.randomUUID().toString())
                .username("superadmin@test.hdim.io")
                .roles("SUPER_ADMIN", "ADMIN", "EVALUATOR", "ANALYST", "VIEWER")
                .tenantIds(primaryTenantId)
                .build();
    }

    /**
     * Creates headers for an ADMIN user.
     *
     * @param tenantId the tenant ID
     * @return HttpHeaders with admin authentication
     */
    public static HttpHeaders adminHeaders(String tenantId) {
        return builder()
                .tenantId(tenantId)
                .userId(UUID.randomUUID().toString())
                .username("admin@test.hdim.io")
                .roles("ADMIN", "EVALUATOR", "ANALYST", "VIEWER")
                .tenantIds(tenantId)
                .build();
    }

    /**
     * Creates headers for an EVALUATOR user.
     *
     * @param tenantId the tenant ID
     * @return HttpHeaders with evaluator authentication
     */
    public static HttpHeaders evaluatorHeaders(String tenantId) {
        return builder()
                .tenantId(tenantId)
                .userId(UUID.randomUUID().toString())
                .username("evaluator@test.hdim.io")
                .roles("EVALUATOR", "ANALYST", "VIEWER")
                .tenantIds(tenantId)
                .build();
    }

    /**
     * Creates headers for an ANALYST user.
     *
     * @param tenantId the tenant ID
     * @return HttpHeaders with analyst authentication
     */
    public static HttpHeaders analystHeaders(String tenantId) {
        return builder()
                .tenantId(tenantId)
                .userId(UUID.randomUUID().toString())
                .username("analyst@test.hdim.io")
                .roles("ANALYST", "VIEWER")
                .tenantIds(tenantId)
                .build();
    }

    /**
     * Creates headers for a VIEWER user (read-only access).
     *
     * @param tenantId the tenant ID
     * @return HttpHeaders with viewer authentication
     */
    public static HttpHeaders viewerHeaders(String tenantId) {
        return builder()
                .tenantId(tenantId)
                .userId(UUID.randomUUID().toString())
                .username("viewer@test.hdim.io")
                .roles("VIEWER")
                .tenantIds(tenantId)
                .build();
    }

    /**
     * Creates headers with no authentication (for testing unauthenticated access).
     *
     * @return HttpHeaders with only tenant ID (no auth headers)
     */
    public static HttpHeaders unauthenticatedHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TENANT_ID_HEADER, tenantId);
        return headers;
    }

    // =========================================================================
    // Builder Pattern for Custom Headers
    // =========================================================================

    /**
     * Creates a new HeaderBuilder for custom header configuration.
     *
     * @return a new HeaderBuilder instance
     */
    public static HeaderBuilder builder() {
        return new HeaderBuilder();
    }

    /**
     * Builder class for constructing custom Gateway Trust headers.
     */
    public static class HeaderBuilder {
        private String tenantId;
        private String userId = UUID.randomUUID().toString();
        private String username = "test.user@test.hdim.io";
        private Set<String> tenantIds = Set.of();
        private Set<String> roles = Set.of("VIEWER");
        private String signingSecret = DEFAULT_TEST_SECRET;
        private boolean includeHmac = false;

        /**
         * Sets the primary tenant ID for the request.
         *
         * @param tenantId the tenant ID
         * @return this builder
         */
        public HeaderBuilder tenantId(String tenantId) {
            this.tenantId = tenantId;
            if (this.tenantIds.isEmpty()) {
                this.tenantIds = Set.of(tenantId);
            }
            return this;
        }

        /**
         * Sets the user ID (UUID format).
         *
         * @param userId the user ID
         * @return this builder
         */
        public HeaderBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the username.
         *
         * @param username the username
         * @return this builder
         */
        public HeaderBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the authorized tenant IDs.
         *
         * @param tenantIds the tenant IDs
         * @return this builder
         */
        public HeaderBuilder tenantIds(String... tenantIds) {
            this.tenantIds = Set.of(tenantIds);
            return this;
        }

        /**
         * Sets the user's roles.
         *
         * @param roles the roles
         * @return this builder
         */
        public HeaderBuilder roles(String... roles) {
            this.roles = Set.of(roles);
            return this;
        }

        /**
         * Enables HMAC signature generation.
         * Use this for testing HMAC validation in gateway service.
         *
         * @param signingSecret the HMAC signing secret
         * @return this builder
         */
        public HeaderBuilder withHmac(String signingSecret) {
            this.includeHmac = true;
            this.signingSecret = signingSecret;
            return this;
        }

        /**
         * Builds the HttpHeaders with all configured values.
         *
         * @return configured HttpHeaders
         */
        public HttpHeaders build() {
            if (tenantId == null || tenantId.isEmpty()) {
                throw new IllegalStateException("tenantId is required");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(TENANT_ID_HEADER, tenantId);
            headers.add(AUTH_USER_ID_HEADER, userId);
            headers.add(AUTH_USERNAME_HEADER, username);
            headers.add(AUTH_TENANT_IDS_HEADER, String.join(",", tenantIds));
            headers.add(AUTH_ROLES_HEADER, String.join(",", roles));

            if (includeHmac) {
                String signature = generateHmacSignature(userId, tenantId, roles, signingSecret);
                headers.add(AUTH_VALIDATED_HEADER, signature);
            } else {
                // For development/testing mode, include a simple validated header with the gateway prefix
                // This allows TrustedHeaderAuthFilter in development mode to accept the headers
                long timestamp = System.currentTimeMillis() / 1000;
                headers.add(AUTH_VALIDATED_HEADER, "gateway-" + timestamp + "-dev-signature");
            }

            return headers;
        }
    }

    // =========================================================================
    // MockHttpServletRequest Utilities
    // =========================================================================

    /**
     * Applies Gateway Trust headers to a MockHttpServletRequest.
     *
     * @param request  the request to modify
     * @param tenantId the tenant ID
     * @param roles    the user's roles
     */
    public static void applyHeaders(MockHttpServletRequest request, String tenantId, String... roles) {
        request.addHeader(TENANT_ID_HEADER, tenantId);
        request.addHeader(AUTH_USER_ID_HEADER, UUID.randomUUID().toString());
        request.addHeader(AUTH_USERNAME_HEADER, "test.user@test.hdim.io");
        request.addHeader(AUTH_TENANT_IDS_HEADER, tenantId);
        request.addHeader(AUTH_ROLES_HEADER, String.join(",", roles));
        // Add validated header for development mode
        long timestamp = System.currentTimeMillis() / 1000;
        request.addHeader(AUTH_VALIDATED_HEADER, "gateway-" + timestamp + "-dev-signature");
    }

    /**
     * Applies Gateway Trust headers to a MockHttpServletRequestBuilder.
     *
     * @param builder  the request builder
     * @param tenantId the tenant ID
     * @param roles    the user's roles
     * @return the modified builder
     */
    public static MockHttpServletRequestBuilder applyHeaders(
            MockHttpServletRequestBuilder builder,
            String tenantId,
            String... roles) {

        long timestamp = System.currentTimeMillis() / 1000;
        return builder
                .header(TENANT_ID_HEADER, tenantId)
                .header(AUTH_USER_ID_HEADER, UUID.randomUUID().toString())
                .header(AUTH_USERNAME_HEADER, "test.user@test.hdim.io")
                .header(AUTH_TENANT_IDS_HEADER, tenantId)
                .header(AUTH_ROLES_HEADER, String.join(",", roles))
                .header(AUTH_VALIDATED_HEADER, "gateway-" + timestamp + "-dev-signature");
    }

    /**
     * Applies Gateway Trust headers to HttpHeaders.
     *
     * @param headers  the headers to modify
     * @param tenantId the tenant ID
     * @param roles    the user's roles
     */
    public static void applyHeaders(HttpHeaders headers, String tenantId, String... roles) {
        headers.add(TENANT_ID_HEADER, tenantId);
        headers.add(AUTH_USER_ID_HEADER, UUID.randomUUID().toString());
        headers.add(AUTH_USERNAME_HEADER, "test.user@test.hdim.io");
        headers.add(AUTH_TENANT_IDS_HEADER, tenantId);
        headers.add(AUTH_ROLES_HEADER, String.join(",", roles));
        // Add validated header for development mode
        long timestamp = System.currentTimeMillis() / 1000;
        headers.add(AUTH_VALIDATED_HEADER, "gateway-" + timestamp + "-dev-signature");
    }

    // =========================================================================
    // HMAC Signature Generation (for Gateway Service Testing)
    // =========================================================================

    /**
     * Generates an HMAC signature for the X-Auth-Validated header.
     * This is used by the Gateway Service to prove header authenticity.
     *
     * @param userId        the user ID
     * @param tenantId      the tenant ID
     * @param roles         the user's roles
     * @param signingSecret the HMAC signing secret (min 32 characters)
     * @return Base64-encoded HMAC signature
     */
    public static String generateHmacSignature(
            String userId,
            String tenantId,
            Set<String> roles,
            String signingSecret) {

        if (signingSecret == null || signingSecret.length() < 32) {
            throw new IllegalArgumentException("Signing secret must be at least 32 characters");
        }

        try {
            // Build the message to sign: userId|tenantId|roles|timestamp
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String rolesString = roles.stream().sorted().collect(Collectors.joining(","));
            String message = String.join("|", userId, tenantId, rolesString, timestamp);

            // Generate HMAC-SHA256
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    signingSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hmacBytes);

            // Return signature with timestamp for verification
            return timestamp + "." + signature;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Validates an HMAC signature from the X-Auth-Validated header.
     * Use this in tests to verify signature generation.
     *
     * @param signatureWithTimestamp the signature from X-Auth-Validated header
     * @param userId                 the user ID
     * @param tenantId               the tenant ID
     * @param roles                  the user's roles
     * @param signingSecret          the HMAC signing secret
     * @param maxAgeSeconds          maximum age of the signature in seconds
     * @return true if the signature is valid
     */
    public static boolean validateHmacSignature(
            String signatureWithTimestamp,
            String userId,
            String tenantId,
            Set<String> roles,
            String signingSecret,
            long maxAgeSeconds) {

        if (signatureWithTimestamp == null || !signatureWithTimestamp.contains(".")) {
            return false;
        }

        String[] parts = signatureWithTimestamp.split("\\.", 2);
        if (parts.length != 2) {
            return false;
        }

        try {
            long timestamp = Long.parseLong(parts[0]);
            long now = Instant.now().toEpochMilli();

            // Check timestamp is not too old
            if (now - timestamp > maxAgeSeconds * 1000) {
                return false;
            }

            // Regenerate and compare
            String expectedSignature = generateHmacSignature(userId, tenantId, roles, signingSecret);
            String expectedSignaturePart = expectedSignature.substring(expectedSignature.indexOf('.') + 1);

            return parts[1].equals(expectedSignaturePart);

        } catch (NumberFormatException e) {
            return false;
        }
    }
}

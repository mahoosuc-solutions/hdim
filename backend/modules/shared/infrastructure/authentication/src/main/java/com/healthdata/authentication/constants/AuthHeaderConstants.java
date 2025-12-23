package com.healthdata.authentication.constants;

/**
 * Constants for authentication-related HTTP headers.
 *
 * These headers are used for gateway-to-service authentication propagation.
 * The gateway validates JWT tokens and injects these trusted headers for
 * backend services to extract user context without re-validating tokens.
 *
 * SECURITY: These headers should ONLY be trusted when coming from the gateway.
 * The gateway MUST strip any externally-provided headers with these names
 * before processing the request.
 *
 * Header Flow:
 * 1. Client sends: Authorization: Bearer <jwt>
 * 2. Gateway validates JWT and extracts claims
 * 3. Gateway injects: X-Auth-User-Id, X-Auth-Tenant-Ids, X-Auth-Roles, X-Auth-Validated
 * 4. Backend service reads trusted headers (no JWT re-validation needed)
 */
public final class AuthHeaderConstants {

    private AuthHeaderConstants() {
        // Prevent instantiation
    }

    /**
     * Prefix for all authentication headers injected by the gateway.
     */
    public static final String AUTH_HEADER_PREFIX = "X-Auth-";

    /**
     * Header containing the authenticated user's unique ID (UUID).
     * Example: X-Auth-User-Id: 550e8400-e29b-41d4-a716-446655440000
     */
    public static final String HEADER_USER_ID = "X-Auth-User-Id";

    /**
     * Header containing the authenticated user's username.
     * Example: X-Auth-Username: john.doe
     */
    public static final String HEADER_USERNAME = "X-Auth-Username";

    /**
     * Header containing comma-separated tenant IDs the user has access to.
     * Example: X-Auth-Tenant-Ids: tenant-001,tenant-002
     */
    public static final String HEADER_TENANT_IDS = "X-Auth-Tenant-Ids";

    /**
     * Header containing comma-separated roles assigned to the user.
     * Example: X-Auth-Roles: ADMIN,PROVIDER,VIEWER
     */
    public static final String HEADER_ROLES = "X-Auth-Roles";

    /**
     * Header indicating that the gateway has validated the request.
     * Value is a signature/token that proves the headers came from the gateway.
     * Example: X-Auth-Validated: gateway-signature-token
     *
     * SECURITY: Backend services MUST verify this header to ensure headers
     * were injected by the gateway and not forged by an external client.
     */
    public static final String HEADER_VALIDATED = "X-Auth-Validated";

    /**
     * Header containing the original JWT token ID (jti claim).
     * Used for audit logging and token revocation.
     * Example: X-Auth-Token-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
     */
    public static final String HEADER_TOKEN_ID = "X-Auth-Token-Id";

    /**
     * Header containing the token expiration timestamp (epoch seconds).
     * Example: X-Auth-Token-Expires: 1703260800
     */
    public static final String HEADER_TOKEN_EXPIRES = "X-Auth-Token-Expires";

    /**
     * Request attribute key for storing extracted tenant IDs.
     * Used by TenantAccessFilter for tenant isolation.
     */
    public static final String ATTR_TENANT_IDS = "userTenantIds";

    /**
     * Request attribute key for storing extracted user ID.
     */
    public static final String ATTR_USER_ID = "userId";

    /**
     * Request attribute key for storing extracted username.
     */
    public static final String ATTR_USERNAME = "username";

    /**
     * Request attribute key for storing extracted roles.
     */
    public static final String ATTR_ROLES = "userRoles";

    /**
     * Delimiter used for separating multiple values in headers (tenant IDs, roles).
     */
    public static final String VALUE_DELIMITER = ",";

    /**
     * Expected value prefix for the validated header signature.
     * Format: gateway-{timestamp}-{hmac}
     */
    public static final String VALIDATED_SIGNATURE_PREFIX = "gateway-";

    /**
     * Check if a header name is an auth header that should be stripped from external requests.
     *
     * @param headerName header name to check
     * @return true if header is an auth header
     */
    public static boolean isAuthHeader(String headerName) {
        return headerName != null && headerName.startsWith(AUTH_HEADER_PREFIX);
    }

    /**
     * Get all auth header names that should be stripped from incoming requests.
     *
     * @return array of auth header names
     */
    public static String[] getAllAuthHeaders() {
        return new String[]{
            HEADER_USER_ID,
            HEADER_USERNAME,
            HEADER_TENANT_IDS,
            HEADER_ROLES,
            HEADER_VALIDATED,
            HEADER_TOKEN_ID,
            HEADER_TOKEN_EXPIRES
        };
    }
}

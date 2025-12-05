package com.healthdata.shared.security.jwt;

/**
 * JWT Constants - Defines JWT-related constants used across the application
 *
 * Contains claim names, token prefixes, and security configuration constants.
 */
public class JwtConstants {

    private JwtConstants() {
        // Utility class - prevent instantiation
    }

    // Token Claims
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_PERMISSIONS = "permissions";

    // Token Types
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // Authorization Header
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_BEARER_PREFIX = "Bearer ";

    // User Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PROVIDER = "PROVIDER";
    public static final String ROLE_CARE_MANAGER = "CARE_MANAGER";
    public static final String ROLE_PATIENT = "PATIENT";
    public static final String ROLE_SYSTEM = "SYSTEM";

    // Error Messages
    public static final String ERROR_INVALID_TOKEN = "Invalid JWT token";
    public static final String ERROR_EXPIRED_TOKEN = "JWT token is expired";
    public static final String ERROR_UNSUPPORTED_TOKEN = "JWT token is unsupported";
    public static final String ERROR_EMPTY_TOKEN = "JWT claims string is empty";
    public static final String ERROR_INVALID_SIGNATURE = "Invalid JWT signature";
    public static final String ERROR_MALFORMED_TOKEN = "Malformed JWT token";

    // Token Validation
    public static final int MINIMUM_TOKEN_LENGTH = 10;
    public static final int MAXIMUM_TOKEN_LENGTH = 5000;

    // Security Headers
    public static final String HEADER_CSRF_TOKEN = "X-CSRF-Token";
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_REQUEST_ID = "X-Request-ID";
}

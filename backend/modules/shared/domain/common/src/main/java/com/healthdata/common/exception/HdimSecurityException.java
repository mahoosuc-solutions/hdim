package com.healthdata.common.exception;

/**
 * Exception for security-related errors.
 *
 * Use this exception hierarchy for:
 * - Authentication failures
 * - Authorization/permission errors
 * - Security configuration errors
 * - Token validation errors
 * - Multi-tenant access violations
 *
 * IMPORTANT: Never include sensitive details like passwords, tokens, or PHI in messages.
 *
 * HTTP Status: 401, 403
 */
public class HdimSecurityException extends HdimException {

    public HdimSecurityException(String message) {
        super("HDIM-SEC-000", message);
    }

    public HdimSecurityException(String message, Throwable cause) {
        super("HDIM-SEC-000", message, cause);
    }

    public HdimSecurityException(String errorCode, String message) {
        super(errorCode, message);
    }

    public HdimSecurityException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public HdimSecurityException(String errorCode, String message, String correlationId) {
        super(errorCode, message, correlationId);
    }

    @Override
    public int getHttpStatus() {
        return 403;
    }

    // ==================== Specialized Subclasses ====================

    /**
     * Exception thrown when authentication fails.
     * HTTP Status: 401
     */
    public static class AuthenticationException extends HdimSecurityException {
        public AuthenticationException(String message) {
            super("HDIM-SEC-401", message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super("HDIM-SEC-401", message, cause);
        }

        @Override
        public int getHttpStatus() {
            return 401;
        }
    }

    /**
     * Exception thrown when a user lacks permission for an operation.
     * HTTP Status: 403
     */
    public static class AuthorizationException extends HdimSecurityException {
        private final String requiredPermission;
        private final String resource;

        public AuthorizationException(String message) {
            super("HDIM-SEC-403", message);
            this.requiredPermission = null;
            this.resource = null;
        }

        public AuthorizationException(String requiredPermission, String resource) {
            super("HDIM-SEC-403", "Access denied to resource: " + resource);
            this.requiredPermission = requiredPermission;
            this.resource = resource;
        }

        public String getRequiredPermission() {
            return requiredPermission;
        }

        public String getResource() {
            return resource;
        }

        @Override
        public int getHttpStatus() {
            return 403;
        }
    }

    /**
     * Exception thrown when tenant access is violated.
     * HTTP Status: 403
     */
    public static class TenantAccessException extends HdimSecurityException {
        private final String attemptedTenantId;

        public TenantAccessException(String message) {
            super("HDIM-SEC-403", message);
            this.attemptedTenantId = null;
        }

        public TenantAccessException(String attemptedTenantId, String message) {
            super("HDIM-SEC-403", message);
            this.attemptedTenantId = attemptedTenantId;
        }

        public String getAttemptedTenantId() {
            return attemptedTenantId;
        }

        @Override
        public int getHttpStatus() {
            return 403;
        }
    }

    /**
     * Exception thrown when security configuration is invalid.
     * HTTP Status: 500 (internal error, not client's fault)
     */
    public static class SecurityConfigurationException extends HdimSecurityException {
        public SecurityConfigurationException(String message) {
            super("HDIM-SEC-500", message);
        }

        public SecurityConfigurationException(String message, Throwable cause) {
            super("HDIM-SEC-500", message, cause);
        }

        @Override
        public int getHttpStatus() {
            return 500;
        }
    }

    /**
     * Exception thrown when JWT token validation fails.
     * HTTP Status: 401
     */
    public static class TokenValidationException extends HdimSecurityException {
        private final String reason;

        public TokenValidationException(String reason) {
            super("HDIM-SEC-401", "Token validation failed: " + reason);
            this.reason = reason;
        }

        public TokenValidationException(String reason, Throwable cause) {
            super("HDIM-SEC-401", "Token validation failed: " + reason, cause);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public int getHttpStatus() {
            return 401;
        }
    }
}

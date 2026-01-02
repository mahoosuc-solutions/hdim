package com.healthdata.common.exception;

/**
 * Base exception class for the Health Data in Motion (HDIM) platform.
 *
 * All custom exceptions in the platform should extend this class or one of its
 * specialized subclasses. This enables:
 * - Consistent error handling across all services
 * - Structured error responses in APIs
 * - Proper error categorization for monitoring and alerting
 * - HIPAA-compliant error logging (no PHI in messages)
 *
 * Exception Hierarchy:
 * - HdimException (this class)
 *   - HdimBusinessException: Business logic and validation errors
 *   - HdimSecurityException: Authentication and authorization errors
 *   - HdimIntegrationException: External service and integration errors
 *   - HdimSystemException: Internal system and configuration errors
 *
 * Usage:
 * - Include error codes for machine-readable error identification
 * - Never include PHI (Protected Health Information) in error messages
 * - Use correlation IDs for distributed tracing
 */
public class HdimException extends RuntimeException {

    private final String errorCode;
    private final String correlationId;

    public HdimException(String message) {
        super(message);
        this.errorCode = "HDIM-000";
        this.correlationId = null;
    }

    public HdimException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "HDIM-000";
        this.correlationId = null;
    }

    public HdimException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = null;
    }

    public HdimException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = null;
    }

    public HdimException(String errorCode, String message, String correlationId) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    public HdimException(String errorCode, String message, String correlationId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns the HTTP status code that should be used for this exception.
     * Override in subclasses to specify appropriate status codes.
     *
     * @return HTTP status code (default: 500)
     */
    public int getHttpStatus() {
        return 500;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [errorCode=").append(errorCode);
        if (correlationId != null) {
            sb.append(", correlationId=").append(correlationId);
        }
        sb.append("]: ").append(getMessage());
        return sb.toString();
    }
}
